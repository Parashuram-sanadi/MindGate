"""
api/schemas.py
Pydantic v2 request and response schemas for the prediction API.
"""

from pydantic import BaseModel, Field, field_validator, model_validator
from typing   import Optional, List, Union, Any
from datetime import datetime


# ─── Request Schema ───────────────────────────────────────────────────────────

class PredictionRequest(BaseModel):
    """
    Full prediction request matching all 38 raw features from the survey.
    """
    # Profile
    Q1_AgeGroup:             Union[str, int, float] = Field(..., json_schema_extra={"example": "19-21"})
    Q2_Gender:               Union[str, int, float] = Field(..., json_schema_extra={"example": "Male"})
    Q3_YearOfStudy:          Union[str, int, float] = Field(..., json_schema_extra={"example": "2nd Year"})
    Q4_Department:           Union[str, int, float] = Field(..., json_schema_extra={"example": "CSE"})
    Q5_Residence:            Union[str, int, float] = Field(..., json_schema_extra={"example": "Hostel"})

    # Usage
    Q6_PrimaryGadget:        Union[str, int, float] = Field(..., json_schema_extra={"example": "Smartphone"})
    Q7_ScreenTime:           Union[str, int, float] = Field(..., json_schema_extra={"example": "5-7"})
    Q8_GadgetUses:           Union[str, int, float] = Field(..., description="Comma-separated uses", json_schema_extra={"example": "Social Media,Gaming"})
    Q9_SocialPlatforms:      Union[str, int, float] = Field(..., json_schema_extra={"example": "4-5"})
    Q11_PhoneChecks_PerHr:   Union[str, int, float] = Field(..., json_schema_extra={"example": "6-10"})
    Q15_GadgetFreeDay:       Union[str, int, float] = Field(..., json_schema_extra={"example": "Sometimes"})

    # Sleep & Physical
    sleep_hours:             float = Field(..., ge=0, le=24)
    Q23_SleepDuration:       Union[str, int, float] = Field(..., json_schema_extra={"example": "6-7"})
    Q24_GadgetBefore30minSleep: Union[str, int, float] = Field(..., json_schema_extra={"example": "Often"})
    Q25_WakesForPhone:       Union[str, int, float] = Field(..., json_schema_extra={"example": "Rarely"})
    Q26_SleepQuality:        Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q27_ExercisePerWeek:     Union[str, int, float] = Field(..., json_schema_extra={"example": "1-3"})
    Q28_PhysicalSymptoms:    Union[str, int, float] = Field(..., description="Comma-separated symptoms", json_schema_extra={"example": "Eye strain,Headaches"})
    Q29_SkipsMeals:          Union[str, int, float] = Field(..., json_schema_extra={"example": "Sometimes"})

    # Academic
    Q16_CGPA:                Union[str, int, float] = Field(..., json_schema_extra={"example": "7-8"})
    Q17_ClassesMissed:       Union[str, int, float] = Field(..., json_schema_extra={"example": "0-2"})
    Q18_GadgetDistractsStudy: Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q19_SelfStudyHrs:        Union[str, int, float] = Field(..., json_schema_extra={"example": "2-3"})
    Q20_ProductivityRating:  Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q21_GadgetHelpsAcademic: Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q22_AssignmentsOnTime:   Union[str, int, float] = Field(..., json_schema_extra={"example": "Usually"})

    # Psychological
    Q13_AnxiousWithoutGadget: Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q30_StressLevel:          Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q12_PhantomCheckUrge:     Union[str, int, float] = Field(..., json_schema_extra={"example": "Sometimes"})
    Q33_EmotionalCoping:      Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q34_UsageOutOfControl:    Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q37_SelfLabel:            Union[str, int, float] = Field(..., json_schema_extra={"example": "No"})
    Q38_SatisfactionRating:   Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q36_TriedToReduce:        Union[str, int, float] = Field(..., json_schema_extra={"example": "Tried and failed"})

    # Social
    Q10_LateNightUsage:      Union[str, int, float] = Field(..., json_schema_extra={"example": "Often"})
    Q14_GadgetAtMeals:       Union[str, int, float] = Field(..., json_schema_extra={"example": "Sometimes"})
    Q31_SocialInteractionQuality: Union[str, int, float] = Field(..., json_schema_extra={"example": 3})
    Q32_PrefersOnline:       Union[str, int, float] = Field(..., json_schema_extra={"example": "Sometimes"})
    Q35_FeltNeedToReduce:     Union[str, int, float] = Field(..., json_schema_extra={"example": "Yes sometimes"})

    # Gate override (only for validated post-processing flows)
    gate1_score_valid:  Optional[bool] = None
    gate2_danger_met:   Optional[bool] = None
    gate3_loss_control: Optional[bool] = None

    @model_validator(mode="before")
    @classmethod
    def preprocess_and_fill_defaults(cls, data: Any) -> Any:
        if isinstance(data, dict):
            # 1. Map friendly aliases
            alias_map = {
                "screen_time": "Q7_ScreenTime",
                "phone_checks": "Q11_PhoneChecks_PerHr",
                "stress_level": "Q30_StressLevel",
                "late_night_usage": "Q10_LateNightUsage"
            }
            for friendly, q_field in alias_map.items():
                if friendly in data and q_field not in data:
                    data[q_field] = data[friendly]
            
            # 2. Load defaults
            import os, json
            try:
                base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
                defaults_path = os.path.join(base_dir, "models", "default_inputs.json")
                if os.path.exists(defaults_path):
                    with open(defaults_path) as f:
                        defaults = json.load(f)
                else:
                    defaults = {}
            except Exception:
                defaults = {}
                
            required_fields = [
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
            for field in required_fields:
                if field not in data or data[field] is None or (isinstance(data[field], float) and os.path.exists(defaults_path) and json.load(open(defaults_path)).get(field) is None and field != "sleep_hours" and str(data[field]) == "nan"):
                    # Check for genuine pandas/excel nan
                    import pandas as pd
                    if field == "Q28_PhysicalSymptoms":
                        data[field] = ""
                    elif field in defaults:
                        data[field] = defaults[field]
                elif isinstance(data[field], str) and data[field].strip() == "":
                    if field == "Q28_PhysicalSymptoms":
                        data[field] = ""
                    elif field in defaults:
                        data[field] = defaults[field]
            
            # 3. Sanitize inputs to coerce types and check basic ranges
            from src.preprocessor import sanitize_prediction_input
            try:
                sanitized = sanitize_prediction_input(data, defaults)
                # Update data dict with sanitized values
                for k, v in sanitized.items():
                    data[k] = v
            except ValueError as e:
                raise ValueError(str(e))
                
        return data

    @field_validator("Q7_ScreenTime")
    @classmethod
    def validate_screen_time(cls, v):
        valid = {"<2", "2-3", "3-5", "5-7", "7-9", "9+"}
        if v not in valid:
            raise ValueError(f"Q7_ScreenTime must be one of {valid}")
        return v

    @field_validator("Q11_PhoneChecks_PerHr")
    @classmethod
    def validate_phone_checks(cls, v):
        valid = {"1-2", "3-5", "6-10", "10+"}
        if v not in valid:
            raise ValueError(f"Q11_PhoneChecks_PerHr must be one of {valid}")
        return v


# ─── Response Schema ──────────────────────────────────────────────────────────

class GateResults(BaseModel):
    gate1_score_valid:  bool
    gate2_danger_met:   bool
    gate3_loss_control: bool
    all_gates_pass:     bool


class PredictionResponse(BaseModel):
    risk_level:         str
    risk_score:         float
    confidence:         float
    recommendation:     str
    recommendations:    List[str]
    intervention_level: str
    timestamp:          str
    top_factors:        List[str] = []
    raw_prediction:     Optional[str]  = None
    downgraded:         Optional[bool] = None
    gate_results:       Optional[GateResults] = None
    model_version:      str = "1.0.0"


class BatchRequest(BaseModel):
    records: List[PredictionRequest]


class BatchResponse(BaseModel):
    results:    List[PredictionResponse]
    total:      int
    processed:  int
    failed:     int


class BatchFileEntry(BaseModel):
    student_info: dict
    prediction: PredictionResponse


class BatchFileResponse(BaseModel):
    results: List[BatchFileEntry]
    summary: dict


class HealthResponse(BaseModel):
    status:         str
    model_loaded:   bool
    model_name:     str
    version:        str
    uptime_seconds: float


# ─── Parent Monitoring Extension ──────────────────────────────────────────────

class LinkTokenResponse(BaseModel):
    code: str
    expires_at: datetime
    ttl_seconds: int

class LinkAcceptRequest(BaseModel):
    code: str

class LinkAcceptResponse(BaseModel):
    student_id: int
    student_name: str
    linked_at: datetime
    student_firebase_uid: Optional[str] = None

class ConnectionStatusResponse(BaseModel):
    is_linked: bool
    role: str
    linked_count: Optional[int] = None
    linked_parent_id: Optional[int] = None
    linked_parent_firebase_uid: Optional[str] = None
    linked_student_ids: Optional[List[int]] = None

class ChildResponse(BaseModel):
    student_id: int
    name: str
    status: str

class ChildStatusResponse(BaseModel):
    today_usage_seconds: int
    daily_limit_seconds: int
    exceeded: bool
    exceeded_by_seconds: Optional[int] = None

class AppUsage(BaseModel):
    package: str
    label: str
    seconds: int

class UsageEvent(BaseModel):
    type: str
    timestamp: datetime

class ChildUsageResponse(BaseModel):
    date: str
    total_seconds: int
    limit_seconds: int
    by_app: List[AppUsage]
    events: List[UsageEvent]

class LimitEventRequest(BaseModel):
    app_package: Optional[str] = None
    limit_seconds: int
    actual_usage_seconds: int
    timestamp: datetime
    event_date: str # YYYY-MM-DD

class LimitEventResponse(BaseModel):
    event_id: int
    notification: str # sent | suppressed_duplicate | no_parent_linked

class DeviceRegistrationRequest(BaseModel):
    device_id: str
    push_token: str
    platform: str = "android"

class SelectRoleRequest(BaseModel):
    role: str # student | parent

class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str
    user_id: int
    name: str
    email: str
    firebase_token: Optional[str] = None

class GoogleAuthRequest(BaseModel):
    id_token: str

class GoogleAuthCompleteRequest(BaseModel):
    id_token: str
    role: str # student | parent
    name: Optional[str] = None

class UsageSyncItem(BaseModel):
    package: str
    label: str
    seconds: int

class FullUsageSyncRequest(BaseModel):
    date: str # YYYY-MM-DD
    usage: List[UsageSyncItem]
