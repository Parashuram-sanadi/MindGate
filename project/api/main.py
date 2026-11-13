"""
api/main.py
Production-grade FastAPI backend for Gadget Addiction Prediction.

Run:  uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
Docs: http://localhost:8000/docs
"""

import os, sys, time
from contextlib import asynccontextmanager
from typing import List
from datetime import datetime, timezone, timedelta, date

from fastapi             import FastAPI, HTTPException, Depends, BackgroundTasks, File, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses   import JSONResponse
import pandas as pd
import io
from pydantic import BaseModel, EmailStr

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from api.schemas       import (PredictionRequest, PredictionResponse,
                                BatchRequest, BatchResponse, HealthResponse,
                                BatchFileResponse, BatchFileEntry,
                                LinkTokenResponse, LinkAcceptRequest, LinkAcceptResponse,
                                ChildResponse, ChildStatusResponse, AppUsage, UsageEvent,
                                ChildUsageResponse, LimitEventRequest, LimitEventResponse,
                                DeviceRegistrationRequest, SelectRoleRequest, AuthResponse,
                                GoogleAuthRequest, GoogleAuthCompleteRequest,
                                ConnectionStatusResponse, FullUsageSyncRequest, UsageSyncItem)
from api.model_service import get_service, ModelService
from api.database import engine, get_db, SessionLocal
from api.models import Base, ParentChildRelation, ParentLinkToken, DeviceRegistration, UsageLimitEvent, NotificationEvent, User, ActiveLimitSession, UsageSnapshot
from api.auth import get_current_user, authenticate_user, create_access_token, ACCESS_TOKEN_EXPIRE_MINUTES
from api.firebase_utils import generate_firebase_token, get_firebase_app
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
import secrets
import hashlib

# ─── App Lifecycle ────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load model at startup and init DB."""
    print("[API] Initialising database ...")
    Base.metadata.create_all(bind=engine)

    # Backfill existing users (§8)
    db = SessionLocal()
    try:
        db.query(User).filter(User.role_selected == False).update({"role_selected": True})
        db.commit()
    finally:
        db.close()

    # Initialize Firebase Admin SDK
    get_firebase_app()

    print("[API] Initialising model service ...")
    get_service()
    print("[API] Model service ready.")
    yield
    print("[API] Shutting down.")


# ─── App Instance ─────────────────────────────────────────────────────────────

app = FastAPI(
    title="Electronic Gadget Addiction Prediction API",
    description=(
        "Production ML API for predicting student gadget addiction risk levels "
        "(Low / Moderate / High / Severe) and generating personalised digital "
        "wellness recommendations."
    ),
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# ─── CORS ─────────────────────────────────────────────────────────────────────

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],           # Restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── Dependency ───────────────────────────────────────────────────────────────

def get_model_service() -> ModelService:
    return get_service()


# ─── Auth Endpoint ─────────────────────────────────────────────────────────────

from fastapi.security import OAuth2PasswordRequestForm

@app.post("/token", tags=["Auth"], response_model=AuthResponse)
async def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = authenticate_user(db, form_data.username, form_data.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.email}, expires_delta=access_token_expires
    )

    # Generate Firebase Custom Token
    firebase_token = generate_firebase_token(user.firebase_uid or user.id, user.role)

    return {
        "access_token": access_token,
        "token_type": "bearer",
        "role": user.role,
        "user_id": user.id,
        "name": user.name,
        "email": user.email,
        "firebase_token": firebase_token
    }

from pydantic import EmailStr
class UserCreate(BaseModel):
    name: str
    email: EmailStr
    password: str
    role: str # student | parent

@app.post("/api/register", tags=["Auth"])
async def register_user(user_in: UserCreate, db: Session = Depends(get_db)):
    try:
        from api.auth import get_password_hash
        db_user = db.query(User).filter(User.email == user_in.email).first()
        if db_user:
            raise HTTPException(status_code=400, detail="Email already registered")

        if user_in.role not in ["student", "parent", "teacher"]:
            raise HTTPException(status_code=400, detail="Invalid role")

        new_user = User(
            name=user_in.name,
            email=user_in.email,
            password_hash=get_password_hash(user_in.password),
            role=user_in.role,
            role_selected=True
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)
        return {"message": "User created", "id": new_user.id}
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/user/select-role", tags=["Auth"])
async def select_role(payload: SelectRoleRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role_selected:
        raise HTTPException(status_code=409, detail="Role already selected for this account")
    if payload.role not in ("student", "parent"):
        raise HTTPException(status_code=400, detail="Invalid role")
    current_user.role = payload.role
    current_user.role_selected = True
    db.commit()
    return {"role": current_user.role}

@app.post("/api/auth/google", tags=["Auth"])
async def google_auth(payload: GoogleAuthRequest, db: Session = Depends(get_db)):
    """Verifies a Google ID token and logs the user in or signals role selection."""
    from google.oauth2 import id_token
    from google.auth.transport import requests as google_requests

    CLIENT_ID = "667786982333-okvfhf9gqp9c220rt5ocl33qh5mnfi3c.apps.googleusercontent.com"

    try:
        # Verify the ID token using google-auth library (Android sends Google ID Token)
        decoded_token = id_token.verify_oauth2_token(payload.id_token, google_requests.Request(), CLIENT_ID)

        email = decoded_token.get("email")
        if not email:
            raise HTTPException(status_code=400, detail="ID token does not contain email")

        user = db.query(User).filter(User.email == email).first()
        if user:
            # Sync Firebase UID if missing
            if not user.firebase_uid:
                user.firebase_uid = decoded_token.get("sub")
                db.commit()

            # Existing user - return standard response (same as /token)
            access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
            access_token = create_access_token(
                data={"sub": user.email}, expires_delta=access_token_expires
            )
            # Use real Firebase UID for custom token if we have it
            firebase_token = generate_firebase_token(user.firebase_uid or user.id, user.role)
            return {
                "access_token": access_token,
                "token_type": "bearer",
                "role": user.role,
                "user_id": user.id,
                "name": user.name,
                "email": user.email,
                "firebase_token": firebase_token
            }
        else:
            # New user - role required to complete registration
            return {"status": "role_required", "email": email}
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Invalid Google token: {str(e)}")

@app.post("/api/auth/google/complete", tags=["Auth"], response_model=AuthResponse)
async def google_auth_complete(payload: GoogleAuthCompleteRequest, db: Session = Depends(get_db)):
    """Completes registration for a new Google user by assigning a role."""
    from google.oauth2 import id_token
    from google.auth.transport import requests as google_requests
    from api.auth import get_password_hash

    CLIENT_ID = "667786982333-okvfhf9gqp9c220rt5ocl33qh5mnfi3c.apps.googleusercontent.com"

    try:
        decoded_token = id_token.verify_oauth2_token(payload.id_token, google_requests.Request(), CLIENT_ID)
        email = decoded_token.get("email")
        if not email:
            raise HTTPException(status_code=400, detail="ID token does not contain email")

        if payload.role not in ["student", "parent"]:
            raise HTTPException(status_code=400, detail="Invalid role")

        # Check if user already exists
        user = db.query(User).filter(User.email == email).first()
        if not user:
            # Create new user
            name = payload.name or decoded_token.get("name") or email.split("@")[0]
            # Use random secret for Google-only users (won't be used for login)
            fake_password = secrets.token_urlsafe(32)
            user = User(
                name=name,
                email=email,
                password_hash=get_password_hash(fake_password),
                role=payload.role,
                role_selected=True,
                firebase_uid=decoded_token.get("sub")
            )
            db.add(user)
            db.commit()
            db.refresh(user)

        access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = create_access_token(
            data={"sub": user.email}, expires_delta=access_token_expires
        )
        # Use real Firebase UID for custom token if we have it
        firebase_token = generate_firebase_token(user.firebase_uid or user.id, user.role)

        return {
            "access_token": access_token,
            "token_type": "bearer",
            "role": user.role,
            "user_id": user.id,
            "name": user.name,
            "email": user.email,
            "firebase_token": firebase_token
        }
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Authentication failed: {str(e)}")


# ─── Parent Monitoring Auth Dependency ────────────────────────────────────────

def require_linked_parent(student_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role != "parent":
        raise HTTPException(status_code=403, detail="Only parents can access this information.")
    rel = db.query(ParentChildRelation).filter(
        ParentChildRelation.parent_id == current_user.id,
        ParentChildRelation.student_id == student_id,
        ParentChildRelation.status == "active"
    ).first()
    if not rel:
        raise HTTPException(status_code=403, detail="You are not linked to this student.")
    return rel


# ─── Health Check ─────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse, tags=["System"])
async def health_check(svc: ModelService = Depends(get_model_service)):
    """Returns API health status and model info."""
    return HealthResponse(
        status="healthy",
        model_loaded=True,
        model_name=svc.model_name,
        version="1.0.0",
        uptime_seconds=svc.uptime,
    )


@app.get("/api/user/connection-status", response_model=ConnectionStatusResponse, tags=["Parent Monitoring"])
async def get_connection_status(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role == "student":
        rel = db.query(ParentChildRelation).filter(
            ParentChildRelation.student_id == current_user.id,
            ParentChildRelation.status == "active"
        ).first()

        linked_parent_firebase_uid = None
        if rel:
            parent = db.query(User).get(rel.parent_id)
            linked_parent_firebase_uid = parent.firebase_uid or str(parent.id)

        return {
            "is_linked": rel is not None,
            "role": current_user.role,
            "linked_parent_id": rel.parent_id if rel else None,
            "linked_parent_firebase_uid": linked_parent_firebase_uid
        }
    elif current_user.role == "parent":
        rels = db.query(ParentChildRelation).filter(
            ParentChildRelation.parent_id == current_user.id,
            ParentChildRelation.status == "active"
        ).all()
        return {
            "is_linked": len(rels) > 0,
            "linked_count": len(rels),
            "role": current_user.role,
            "linked_student_ids": [r.student_id for r in rels]
        }
    return {"is_linked": False, "role": current_user.role}


@app.get("/", tags=["System"])
async def root():
    return {
        "message": "Gadget Addiction Prediction API",
        "version": "1.0.0",
        "docs": "/docs",
    }


# ─── Single Prediction ────────────────────────────────────────────────────────

@app.post(
    "/predict",
    response_model=PredictionResponse,
    tags=["Prediction"],
    summary="Predict gadget addiction risk level",
    description=(
        "Submit student gadget usage data and receive a risk level prediction "
        "(Low/Moderate/High/Severe), confidence score, risk score, and personalised "
        "wellness recommendations."
    ),
)
async def predict(
    request: PredictionRequest,
    svc: ModelService = Depends(get_model_service),
):
    """
    **Single prediction endpoint.**

    Minimum required fields:
    - `screen_time`: one of `<2`, `2-3`, `3-5`, `5-7`, `7-9`, `9+`
    - `sleep_hours`: float (0–24)
    - `stress_level`: integer 1–5
    - `phone_checks`: one of `1-2`, `3-5`, `6-10`, `10+`

    Optional fields provide richer context for more accurate predictions.
    """
    try:
        result = svc.predict(request)
        return PredictionResponse(**result)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {e}")


# ─── Batch Prediction ─────────────────────────────────────────────────────────

@app.post(
    "/predict/batch",
    response_model=BatchResponse,
    tags=["Prediction"],
    summary="Batch predict for multiple students",
)
async def predict_batch(
    batch: BatchRequest,
    svc: ModelService = Depends(get_model_service),
):
    """
    **Batch prediction endpoint.**
    Process up to 100 student records in a single request.
    """
    if len(batch.records) > 100:
        raise HTTPException(status_code=400, detail="Max 100 records per batch.")

    results   = []
    failed    = 0

    for rec in batch.records:
        try:
            res = svc.predict(rec)
            results.append(PredictionResponse(**res))
        except Exception:
            failed += 1

    return BatchResponse(
        results=results,
        total=len(batch.records),
        processed=len(results),
        failed=failed,
    )


@app.post(
    "/predict/batch/file",
    response_model=BatchFileResponse,
    tags=["Prediction"],
    summary="Batch predict from CSV or XLSX file",
)
async def predict_batch_file(
    file: UploadFile = File(...),
    svc: ModelService = Depends(get_model_service),
):
    """
    **Batch prediction from file.**
    Upload a CSV or XLSX file containing student assessment data.
    The file should have columns matching the survey questions.
    Extra columns (like 'Student ID' or 'Name') will be preserved in the response.
    """
    contents = await file.read()
    filename = file.filename.lower()
    try:
        if filename.endswith(".csv"):
            df = pd.read_csv(io.BytesIO(contents))
        elif filename.endswith((".xlsx", ".xls")):
            df = pd.read_excel(io.BytesIO(contents))
        else:
            raise HTTPException(status_code=400, detail=f"Unsupported file format: {file.filename}. Use CSV or XLSX.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Error reading file: {e}")

    if len(df) > 2000:
        raise HTTPException(status_code=400, detail="Max 2000 rows allowed for batch upload.")

    # Validate columns - We only REQUIRE these. Extra columns are ignored.
    required_cols = [
        "Q1_AgeGroup", "Q2_Gender", "Q3_YearOfStudy", "Q4_Department", "Q5_Residence",
        "Q6_PrimaryGadget", "Q7_ScreenTime", "Q8_GadgetUses", "Q9_SocialPlatforms",
        "Q11_PhoneChecks_PerHr", "Q15_GadgetFreeDay", "sleep_hours", "Q23_SleepDuration",
        "Q24_GadgetBefore30minSleep", "Q25_WakesForPhone", "Q26_SleepQuality",
        "Q27_ExercisePerWeek", "Q28_PhysicalSymptoms", "Q29_SkipsMeals", "Q16_CGPA",
        "Q17_ClassesMissed", "Q18_GadgetDistractsStudy", "Q19_SelfStudyHrs",
        "Q20_ProductivityRating", "Q21_GadgetHelpsAcademic", "Q22_AssignmentsOnTime",
        "Q13_AnxiousWithoutGadget", "Q30_StressLevel", "Q12_PhantomCheckUrge",
        "Q33_EmotionalCoping", "Q34_UsageOutOfControl", "Q37_SelfLabel",
        "Q38_SatisfactionRating", "Q36_TriedToReduce", "Q10_LateNightUsage",
        "Q14_GadgetAtMeals", "Q31_SocialInteractionQuality", "Q32_PrefersOnline",
        "Q35_FeltNeedToReduce"
    ]

    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        raise HTTPException(
            status_code=422,
            detail={
                "error": "Missing required columns",
                "missing_columns": missing
            }
        )

    results = []
    records_to_predict = []
    row_infos = []
    skipped_rows = []

    from src.preprocessor import sanitize_prediction_input

    # Gates might be present in labeled datasets
    gate_cols = ["gate1_score_valid", "gate2_danger_met", "gate3_loss_control"]

    for index, row in df.iterrows():
        try:
            # 1. Build row_dict using only required columns (ignoring extras for prediction)
            row_dict = {}
            for c in required_cols:
                row_dict[c] = row.get(c)

            # Extract gates if present
            for gc in gate_cols:
                if gc in row:
                    gv = row.get(gc)
                    if not pd.isna(gv):
                        if isinstance(gv, (int, float)):
                            row_dict[gc] = bool(gv)
                        else:
                            s_gv = str(gv).lower().strip()
                            row_dict[gc] = s_gv in ("1", "1.0", "true", "yes")

            # 2. Extract identifier columns (extra columns in file)
            # Ignore Predicted_AddictionRisk and Corrected_Label to avoid leakage
            exclude_from_info = required_cols + ["Predicted_AddictionRisk", "Corrected_Label"]
            student_info = {str(k): str(v) for k, v in row.items() if k not in exclude_from_info}

            # Ensure identity
            if not student_info.get("Student ID") and not student_info.get("Name") and not student_info.get("ID"):
                student_info["Row"] = str(index + 1)

            # 3. Sanitize and validate
            try:
                sanitized_data = sanitize_prediction_input(row_dict, svc.defaults)

                # Re-add gates to sanitized_data if they were in row_dict
                for gc in gate_cols:
                    if gc in row_dict:
                        sanitized_data[gc] = row_dict[gc]

                req = PredictionRequest(**sanitized_data)
                records_to_predict.append(req)
                row_infos.append(student_info)
            except ValueError as e:
                error_msg = str(e)
                parts = error_msg.split(":")
                if len(parts) >= 2 and (parts[0].strip().startswith("Q") or parts[0].strip() == "sleep_hours"):
                    field = parts[0].strip()
                    error_msg = f"Row {index + 1}, {field}: couldn't interpret value"
                else:
                    error_msg = f"Row {index + 1}: {error_msg}"
                student_id = str(row.get("Student ID") or row.get("Name") or row.get("ID") or f"Row {index + 1}")
                skipped_rows.append({"student_id": student_id, "row": index + 1, "errors": [error_msg]})
            except Exception as e:
                from pydantic import ValidationError
                error_msgs = []
                if isinstance(e, ValidationError):
                    for err in e.errors():
                        loc = str(err["loc"][-1])
                        error_msgs.append(f"Row {index + 1}, {loc}: couldn't interpret value")
                else:
                    error_msgs.append(f"Row {index + 1}: {e}")
                student_id = str(row.get("Student ID") or row.get("Name") or row.get("ID") or f"Row {index + 1}")
                skipped_rows.append({"student_id": student_id, "row": index + 1, "errors": error_msgs})

        except Exception as e:
            student_id = str(row.get("Student ID") or row.get("Name") or row.get("ID") or f"Row {index + 1}")
            skipped_rows.append({"student_id": student_id, "row": index + 1, "errors": [f"Row {index + 1}: {e}"]})

    # 4. Batch Prediction
    if records_to_predict:
        predictions = svc.predict_batch(records_to_predict)
        for info, pred in zip(row_infos, predictions):
            results.append(BatchFileEntry(
                student_info=info,
                prediction=PredictionResponse(**pred)
            ))

    # Summary
    counts = {"Low": 0, "Moderate": 0, "High": 0, "Severe": 0}
    for r in results:
        counts[r.prediction.risk_level] = counts.get(r.prediction.risk_level, 0) + 1

    return BatchFileResponse(
        results=results,
        summary={
            "total_processed": len(results),
            "total_skipped": len(skipped_rows),
            "risk_distribution": counts,
            "skipped_details": skipped_rows[:10] # Show first 10 errors
        }
    )


# ─── Parent Monitoring Endpoints ──────────────────────────────────────────────

@app.get("/api/user/firebase-token", tags=["Auth"])
async def get_firebase_token(current_user: User = Depends(get_current_user)):
    token = generate_firebase_token(current_user.id, current_user.role)
    return {"firebase_token": token}

@app.post("/api/parent/link/create", response_model=LinkTokenResponse, tags=["Parent Monitoring"])
async def create_link_code(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    # CLEANUP: Delete any existing tokens for this student to avoid DB clutter
    db.query(ParentLinkToken).filter(ParentLinkToken.student_id == current_user.id).delete()
    db.commit()

    # 8-character base32 code
    code = f"MG-{secrets.token_hex(4).upper()}" # Spec example MG-7K4P9X2Q

    token_hash = hashlib.sha256(code.encode()).hexdigest()
    # Exactly 3 minutes (180 seconds)
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=3)

    db_token = ParentLinkToken(
        student_id=current_user.id,
        token_hash=token_hash,
        expires_at=expires_at
    )
    db.add(db_token)
    db.commit()

    # Ensure ISO format with UTC suffix for Android compatibility
    return {
        "code": code,
        "expires_at": expires_at.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "ttl_seconds": 180 # Exactly 3 minutes
    }

@app.post("/api/parent/link/accept", response_model=LinkAcceptResponse, tags=["Parent Monitoring"])
async def accept_link_code(payload: LinkAcceptRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role != "parent":
        raise HTTPException(status_code=403, detail="Only parents can link to students.")

    token_hash = hashlib.sha256(payload.code.encode()).hexdigest()
    db_token = db.query(ParentLinkToken).filter(
        ParentLinkToken.token_hash == token_hash
    ).first()

    if not db_token:
        raise HTTPException(status_code=404, detail="Invalid link code.")

    if db_token.attempt_count >= 5:
        raise HTTPException(status_code=400, detail="Too many attempts. This code is now locked.")

    if db_token.used:
        # Increment attempt even on used code to prevent probing status
        db_token.attempt_count += 1
        db.commit()
        raise HTTPException(status_code=400, detail="That code has already been used.")

    if db_token.expires_at.replace(tzinfo=timezone.utc) < datetime.now(timezone.utc):
        db_token.attempt_count += 1
        db.commit()
        raise HTTPException(status_code=400, detail="That code has expired. Ask your child for a new one.")

    # Check for existing relation
    existing = db.query(ParentChildRelation).filter(
        ParentChildRelation.parent_id == current_user.id,
        ParentChildRelation.student_id == db_token.student_id
    ).first()

    if existing:
        if existing.status == "active":
            raise HTTPException(status_code=409, detail="Already linked to this student.")
        existing.status = "active"
        existing.linked_at = datetime.now(timezone.utc)
    else:
        new_rel = ParentChildRelation(
            parent_id=current_user.id,
            student_id=db_token.student_id,
            linked_at=datetime.now(timezone.utc)
        )
        db.add(new_rel)

    db_token.used = True
    db.commit()

    student = db.query(User).get(db_token.student_id)
    return {
        "student_id": student.id,
        "student_name": student.name,
        "linked_at": datetime.now(timezone.utc),
        "student_firebase_uid": str(student.id)
    }

@app.get("/api/parent/children", response_model=List[ChildResponse], tags=["Parent Monitoring"])
async def list_children(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role != "parent":
        raise HTTPException(status_code=403, detail="Only parents can view linked children.")

    rels = db.query(ParentChildRelation).filter(
        ParentChildRelation.parent_id == current_user.id,
        ParentChildRelation.status == "active"
    ).all()

    results = []
    for rel in rels:
        student = db.query(User).get(rel.student_id)
        results.append({
            "student_id": student.id,
            "name": student.name,
            "status": rel.status
        })
    return results

@app.get("/api/parent/children/{student_id}/status", response_model=ChildStatusResponse, tags=["Parent Monitoring"])
async def get_child_status(student_id: int, rel: ParentChildRelation = Depends(require_linked_parent), db: Session = Depends(get_db)):
    today = date.today()

    # 1. Try snapshot first for live parity
    snapshots = db.query(UsageSnapshot).filter(
        UsageSnapshot.student_id == student_id,
        UsageSnapshot.event_date == today
    ).all()

    if snapshots:
        total_used = sum(s.used_seconds for s in snapshots)
        # We don't have the dynamic limits in the snapshot,
        # so we'll fetch the latest event to get the limit, or use default
        latest_event = db.query(UsageLimitEvent).filter(
            UsageLimitEvent.student_id == student_id,
            UsageLimitEvent.event_date == today
        ).order_by(UsageLimitEvent.timestamp.desc()).first()

        limit_seconds = latest_event.limit_seconds if latest_event else 3600

        return {
            "today_usage_seconds": total_used,
            "daily_limit_seconds": limit_seconds,
            "exceeded": total_used >= limit_seconds,
            "exceeded_by_seconds": max(0, total_used - limit_seconds)
        }

    # 2. Fallback to latest event
    latest_event = db.query(UsageLimitEvent).filter(
        UsageLimitEvent.student_id == student_id,
        UsageLimitEvent.event_date == today
    ).order_by(UsageLimitEvent.timestamp.desc()).first()

    if latest_event:
        return {
            "today_usage_seconds": latest_event.actual_usage_seconds,
            "daily_limit_seconds": latest_event.limit_seconds,
            "exceeded": latest_event.actual_usage_seconds >= latest_event.limit_seconds,
            "exceeded_by_seconds": max(0, latest_event.actual_usage_seconds - latest_event.limit_seconds)
        }

    return {
        "today_usage_seconds": 0,
        "daily_limit_seconds": 3600,
        "exceeded": False,
        "exceeded_by_seconds": 0
    }

@app.get("/api/parent/children/{student_id}/usage", response_model=ChildUsageResponse, tags=["Parent Monitoring"])
async def get_child_usage(student_id: int, rel: ParentChildRelation = Depends(require_linked_parent), db: Session = Depends(get_db)):
    # Privacy guardrail §8: dedicated response schema used
    today = date.today()

    # 1. Fetch live snapshot if available
    snapshots = db.query(UsageSnapshot).filter(
        UsageSnapshot.student_id == student_id,
        UsageSnapshot.event_date == today
    ).all()

    if snapshots:
        total_seconds = sum(s.used_seconds for s in snapshots)
        # Sort apps by usage descending
        by_app = sorted([
            {"package": s.app_package, "label": s.app_label, "seconds": s.used_seconds}
            for s in snapshots
        ], key=lambda x: x["seconds"], reverse=True)
    else:
        # Fallback to events if no snapshot yet
        events = db.query(UsageLimitEvent).filter(
            UsageLimitEvent.student_id == student_id,
            UsageLimitEvent.event_date == today
        ).all()
        total_seconds = sum(e.actual_usage_seconds for e in events)
        app_data = {}
        for e in events:
            if e.app_package not in app_data:
                app_data[e.app_package] = 0
            app_data[e.app_package] += e.actual_usage_seconds
        by_app = [{"package": pkg, "label": pkg.split(".")[-1].capitalize(), "seconds": sec} for pkg, sec in app_data.items()]

    # Fetch events for the timeline
    events = db.query(UsageLimitEvent).filter(
        UsageLimitEvent.student_id == student_id,
        UsageLimitEvent.event_date == today
    ).all()

    return {
        "date": str(today),
        "total_seconds": total_seconds,
        "limit_seconds": 3600, # Default or fetch from profile if added
        "by_app": by_app,
        "events": [{"type": "limit_exceeded", "timestamp": e.timestamp} for e in events]
    }

@app.post("/api/usage/sync-full", status_code=204, tags=["Usage Monitoring"])
async def sync_full_usage(payload: FullUsageSyncRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role != "student":
        raise HTTPException(status_code=403, detail="Only students can sync usage.")

    sync_date = date.fromisoformat(payload.date)

    for item in payload.usage:
        # Upsert logic
        existing = db.query(UsageSnapshot).filter(
            UsageSnapshot.student_id == current_user.id,
            UsageSnapshot.app_package == item.package,
            UsageSnapshot.event_date == sync_date
        ).first()

        if existing:
            existing.used_seconds = item.seconds
            existing.app_label = item.label
            existing.last_updated = datetime.now(timezone.utc)
        else:
            db.add(UsageSnapshot(
                student_id=current_user.id,
                app_package=item.package,
                app_label=item.label,
                used_seconds=item.seconds,
                event_date=sync_date
            ))

    db.commit()
    return None

@app.get("/api/parent/alerts", response_model=List[dict], tags=["Parent Monitoring"])
async def list_parent_alerts(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role != "parent":
        raise HTTPException(status_code=403, detail="Only parents can view alerts.")

    alerts = db.query(NotificationEvent).filter(
        NotificationEvent.parent_id == current_user.id
    ).order_by(NotificationEvent.sent_at.desc()).limit(50).all()

    results = []
    for a in alerts:
        student = db.query(User).get(a.student_id)
        results.append({
            "id": a.id,
            "student_id": a.student_id,
            "student_name": student.name if student else "Unknown",
            "type": a.event_type,
            "timestamp": a.sent_at,
            "status": a.status
        })
    return results
    if current_user.role == "parent":
        rel = db.query(ParentChildRelation).filter(
            ParentChildRelation.parent_id == current_user.id,
            ParentChildRelation.student_id == student_id
        ).first()
    else: # student
        rel = db.query(ParentChildRelation).filter(
            ParentChildRelation.student_id == current_user.id,
            ParentChildRelation.parent_id == student_id # here student_id arg is actually parent_id
        ).first()

    if rel:
        db.delete(rel)
        db.commit()
    return None

@app.post("/api/usage/limit-event", response_model=LimitEventResponse, status_code=202, tags=["Usage Monitoring"])
async def record_limit_event(payload: LimitEventRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if current_user.role != "student":
        raise HTTPException(status_code=403, detail="Only students can report usage events.")

    new_event = UsageLimitEvent(
        student_id=current_user.id,
        app_package=payload.app_package,
        limit_seconds=payload.limit_seconds,
        actual_usage_seconds=payload.actual_usage_seconds,
        timestamp=payload.timestamp,
        event_date=date.fromisoformat(payload.event_date)
    )
    db.add(new_event)
    db.commit()

    # §4.3 20-minute throttled alerts
    # cooldown_bucket = floor(event_epoch_seconds / 1200)
    event_timestamp = payload.timestamp.replace(tzinfo=timezone.utc)
    bucket = int(event_timestamp.timestamp() // 1200)

    notification_status = "no_parent_linked"
    linked_parents = db.query(User).join(
        ParentChildRelation, ParentChildRelation.parent_id == User.id
    ).filter(
        ParentChildRelation.student_id == current_user.id,
        ParentChildRelation.status == "active"
    ).all()

    for parent in linked_parents:
        try:
            notif = NotificationEvent(
                student_id=current_user.id,
                parent_id=parent.id,
                event_type="limit_exceeded",
                cooldown_bucket=bucket,
                status="pending"
            )
            db.add(notif)
            db.commit()

            # If commit succeeds, send push
            exceeded_by = (payload.actual_usage_seconds - payload.limit_seconds) // 60
            push_title = "MindGate Alert"
            push_body = f"Your child's daily app usage limit has been exceeded. ({exceeded_by}m over)"

            print(f"[PUSH] Sending to parent {parent.email}: {push_title} - {push_body}")

            notif.status = "sent"
            notif.sent_at = datetime.now(timezone.utc)
            db.commit()
            notification_status = "sent"

        except IntegrityError:
            db.rollback()
            notification_status = "suppressed_duplicate"
            continue

    return {"event_id": new_event.id, "notification": notification_status}

@app.post("/api/devices/register", status_code=200, tags=["Devices"])
async def register_device(payload: DeviceRegistrationRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    # Set prior registrations for this user to inactive
    db.query(DeviceRegistration).filter(
        DeviceRegistration.user_id == current_user.id
    ).update({"is_active": False})

    existing = db.query(DeviceRegistration).filter(
        DeviceRegistration.user_id == current_user.id,
        DeviceRegistration.device_id == payload.device_id
    ).first()

    if existing:
        existing.push_token = payload.push_token
        existing.is_active = True
    else:
        new_reg = DeviceRegistration(
            user_id=current_user.id,
            device_id=payload.device_id,
            push_token=payload.push_token,
            platform=payload.platform
        )
        db.add(new_reg)

    db.commit()
    return {"status": "registered"}


@app.post("/api/parent/notify-request", tags=["Parent Monitoring"])
async def notify_parent_of_request(payload: dict, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Triggers a push notification to linked parents when a student creates an extension request."""
    if current_user.role != "student":
        raise HTTPException(status_code=403, detail="Only students can trigger parent notifications.")

    app_name = payload.get("appName", "an app")

    # Find all active linked parents
    linked_parents = db.query(User).join(
        ParentChildRelation, ParentChildRelation.parent_id == User.id
    ).filter(
        ParentChildRelation.student_id == current_user.id,
        ParentChildRelation.status == "active"
    ).all()

    if not linked_parents:
        return {"status": "no_parent_linked"}

    for parent in linked_parents:
        # Get parent device tokens
        tokens = [reg.push_token for reg in parent.device_registrations if reg.is_active]
        if tokens:
            from api.firebase_utils import send_push_notification
            send_push_notification(
                tokens=tokens,
                title="Extension Request",
                body=f"{current_user.name} is asking for 15 more minutes of {app_name}.",
                data={
                    "type": "approval_request",
                    "student_name": current_user.name,
                    "app_name": app_name,
                    "student_id": str(current_user.id)
                }
            )

    return {"status": "notified", "parent_count": len(linked_parents)}

@app.get("/risk-levels", tags=["Information"])
async def risk_levels():
    """Returns the risk classification bands used by the model."""
    return {
        "risk_bands": {
            "Low":      {"score_range": "0–34",  "description": "Healthy usage patterns"},
            "Moderate": {"score_range": "35–54", "description": "Mild dependency indicators"},
            "High":     {"score_range": "55–64", "description": "Significant addiction indicators"},
            "Severe":   {"score_range": "65+",   "description": "Severe addiction – intervention required"},
        },
        "danger_gate_note": (
            "Severe classification requires all 3 danger gates to be satisfied. "
            "If gates fail, prediction is downgraded to High."
        ),
    }


# ─── Recommendations Lookup ───────────────────────────────────────────────────

@app.get("/recommendations/{risk_level}", tags=["Information"])
async def get_recommendations_by_level(risk_level: str):
    """Returns full recommendation list for a given risk level."""
    from recommendation_engine import BASE_RECOMMENDATIONS
    risk_level = risk_level.capitalize()
    if risk_level not in BASE_RECOMMENDATIONS:
        raise HTTPException(status_code=404, detail=f"Unknown risk level: {risk_level}")
    return {
        "risk_level":      risk_level,
        "recommendations": BASE_RECOMMENDATIONS[risk_level],
    }


# ─── Exception Handlers ───────────────────────────────────────────────────────

@app.exception_handler(404)
async def not_found_handler(request, exc):
    return JSONResponse(
        status_code=404,
        content={"error": "Endpoint not found", "docs": "/docs"}
    )


@app.exception_handler(500)
async def server_error_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error. Please try again."}
    )
