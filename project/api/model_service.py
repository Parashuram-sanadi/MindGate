"""
api/model_service.py
Singleton model loader – loads artifacts once at startup.
"""

import os, sys, json, time
from typing import List
import numpy as np
import pandas as pd
import joblib
import shap

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from api.schemas           import PredictionRequest
from configs.config        import MODEL_DIR, LABEL_ORDER
from src.preprocessor      import manual_encode
from src.feature_derivation import build_full_record
from risk_engine           import proba_to_risk
from recommendation_engine import get_recommendations, get_primary_recommendation


class ModelService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialised = False
        return cls._instance

    def __init__(self):
        if self._initialised:
            return
        self._load()
        self._initialised = True
        self._start_time  = time.time()

    def _load(self):
        print("[ModelService] Loading model artifacts ...")
        self.model    = joblib.load(os.path.join(MODEL_DIR, "best_model.pkl"))
        self.pipeline = joblib.load(os.path.join(MODEL_DIR, "feature_pipeline.pkl"))
        self.label_encoder = joblib.load(os.path.join(MODEL_DIR, "label_encoder.pkl"))

        with open(os.path.join(MODEL_DIR, "feature_columns.json")) as f:
            self.feat_cols = json.load(f)

        with open(os.path.join(MODEL_DIR, "risk_mapping.json")) as f:
            meta = json.load(f)
            self.model_name = meta.get("best_model_name", "Unknown")

        with open(os.path.join(MODEL_DIR, "default_inputs.json")) as f:
            self.defaults = json.load(f)

        # Initialise SHAP explainer
        print("[ModelService] Initialising SHAP explainer ...")
        self.explainer = shap.TreeExplainer(self.model)

        print(f"[ModelService] Loaded: {self.model_name}")

    @property
    def uptime(self) -> float:
        return round(time.time() - self._start_time, 1)

    def _request_to_df(self, req: PredictionRequest) -> pd.DataFrame:
        """Map PredictionRequest fields to DataFrame matching training columns."""
        # Directly map all Q-fields from request to DataFrame columns
        mapping = {
            "Q1_AgeGroup": req.Q1_AgeGroup,
            "Q2_Gender": req.Q2_Gender,
            "Q3_YearOfStudy": req.Q3_YearOfStudy,
            "Q4_Department": req.Q4_Department,
            "Q5_Residence": req.Q5_Residence,
            "Q6_PrimaryGadget": req.Q6_PrimaryGadget,
            "Q7_ScreenTime": req.Q7_ScreenTime,
            "Q8_GadgetUses": req.Q8_GadgetUses,
            "Q9_SocialPlatforms": req.Q9_SocialPlatforms,
            "Q11_PhoneChecks_PerHr": req.Q11_PhoneChecks_PerHr,
            "Q15_GadgetFreeDay": req.Q15_GadgetFreeDay,
            "sleep_hours": req.sleep_hours,
            "Q23_SleepDuration": req.Q23_SleepDuration,
            "Q24_GadgetBefore30minSleep": req.Q24_GadgetBefore30minSleep,
            "Q25_WakesForPhone": req.Q25_WakesForPhone,
            "Q26_SleepQuality": req.Q26_SleepQuality,
            "Q27_ExercisePerWeek": req.Q27_ExercisePerWeek,
            "Q28_PhysicalSymptoms": req.Q28_PhysicalSymptoms,
            "Q29_SkipsMeals": req.Q29_SkipsMeals,
            "Q16_CGPA": req.Q16_CGPA,
            "Q17_ClassesMissed": req.Q17_ClassesMissed,
            "Q18_GadgetDistractsStudy": req.Q18_GadgetDistractsStudy,
            "Q19_SelfStudyHrs": req.Q19_SelfStudyHrs,
            "Q20_ProductivityRating": req.Q20_ProductivityRating,
            "Q21_GadgetHelpsAcademic": req.Q21_GadgetHelpsAcademic,
            "Q22_AssignmentsOnTime": req.Q22_AssignmentsOnTime,
            "Q13_AnxiousWithoutGadget": req.Q13_AnxiousWithoutGadget,
            "Q30_StressLevel": req.Q30_StressLevel,
            "Q12_PhantomCheckUrge": req.Q12_PhantomCheckUrge,
            "Q33_EmotionalCoping": req.Q33_EmotionalCoping,
            "Q34_UsageOutOfControl": req.Q34_UsageOutOfControl,
            "Q37_SelfLabel": req.Q37_SelfLabel,
            "Q38_SatisfactionRating": req.Q38_SatisfactionRating,
            "Q36_TriedToReduce": req.Q36_TriedToReduce,
            "Q10_LateNightUsage": req.Q10_LateNightUsage,
            "Q14_GadgetAtMeals": req.Q14_GadgetAtMeals,
            "Q31_SocialInteractionQuality": req.Q31_SocialInteractionQuality,
            "Q32_PrefersOnline": req.Q32_PrefersOnline,
            "Q35_FeltNeedToReduce": req.Q35_FeltNeedToReduce,
        }

        # Merge user-supplied fields with derived correlated fields and defaults
        # This will populate daily_screen_time_hours, sleep_disturbances, etc.
        full_input = build_full_record(mapping, self.defaults)
        return pd.DataFrame([full_input])

    def predict(self, req: PredictionRequest) -> dict:
        """Run a single prediction. Returns response dict."""
        return self.predict_batch([req])[0]

    def predict_batch(self, requests: List[PredictionRequest]) -> List[dict]:
        """Run batch prediction. Optimized for speed."""
        from datetime import datetime, timezone
        if not requests:
            return []

        # 1. Convert all requests to a single DataFrame
        all_data = []
        for req in requests:
            mapping = {
                "Q1_AgeGroup": req.Q1_AgeGroup, "Q2_Gender": req.Q2_Gender,
                "Q3_YearOfStudy": req.Q3_YearOfStudy, "Q4_Department": req.Q4_Department,
                "Q5_Residence": req.Q5_Residence, "Q6_PrimaryGadget": req.Q6_PrimaryGadget,
                "Q7_ScreenTime": req.Q7_ScreenTime, "Q8_GadgetUses": req.Q8_GadgetUses,
                "Q9_SocialPlatforms": req.Q9_SocialPlatforms, "Q11_PhoneChecks_PerHr": req.Q11_PhoneChecks_PerHr,
                "Q15_GadgetFreeDay": req.Q15_GadgetFreeDay, "sleep_hours": req.sleep_hours,
                "Q23_SleepDuration": req.Q23_SleepDuration, "Q24_GadgetBefore30minSleep": req.Q24_GadgetBefore30minSleep,
                "Q25_WakesForPhone": req.Q25_WakesForPhone, "Q26_SleepQuality": req.Q26_SleepQuality,
                "Q27_ExercisePerWeek": req.Q27_ExercisePerWeek, "Q28_PhysicalSymptoms": req.Q28_PhysicalSymptoms,
                "Q29_SkipsMeals": req.Q29_SkipsMeals, "Q16_CGPA": req.Q16_CGPA,
                "Q17_ClassesMissed": req.Q17_ClassesMissed, "Q18_GadgetDistractsStudy": req.Q18_GadgetDistractsStudy,
                "Q19_SelfStudyHrs": req.Q19_SelfStudyHrs, "Q20_ProductivityRating": req.Q20_ProductivityRating,
                "Q21_GadgetHelpsAcademic": req.Q21_GadgetHelpsAcademic, "Q22_AssignmentsOnTime": req.Q22_AssignmentsOnTime,
                "Q13_AnxiousWithoutGadget": req.Q13_AnxiousWithoutGadget, "Q30_StressLevel": req.Q30_StressLevel,
                "Q12_PhantomCheckUrge": req.Q12_PhantomCheckUrge, "Q33_EmotionalCoping": req.Q33_EmotionalCoping,
                "Q34_UsageOutOfControl": req.Q34_UsageOutOfControl, "Q37_SelfLabel": req.Q37_SelfLabel,
                "Q38_SatisfactionRating": req.Q38_SatisfactionRating, "Q36_TriedToReduce": req.Q36_TriedToReduce,
                "Q10_LateNightUsage": req.Q10_LateNightUsage, "Q14_GadgetAtMeals": req.Q14_GadgetAtMeals,
                "Q31_SocialInteractionQuality": req.Q31_SocialInteractionQuality, "Q32_PrefersOnline": req.Q32_PrefersOnline,
                "Q35_FeltNeedToReduce": req.Q35_FeltNeedToReduce,
            }
            all_data.append(build_full_record(mapping, self.defaults))

        df     = pd.DataFrame(all_data)
        df_enc = manual_encode(df)

        # 2. Vectorized transform and predict
        try:
            X = self.pipeline.transform(df_enc)
            probas = self.model.predict_proba(X)
        except Exception as e:
            raise ValueError(f"Batch prediction failed: {e}")

        # 3. Process results for each row
        results = []
        now_ts = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

        # Get all SHAP values at once
        try:
            sv_all = self.explainer.shap_values(X)
        except Exception as e:
            print(f"[ModelService] Batch SHAP error: {e}")
            sv_all = None

        for i in range(len(requests)):
            req   = requests[i]
            proba = probas[i]

            risk = proba_to_risk(
                proba, LABEL_ORDER,
                gate1=req.gate1_score_valid,
                gate2=req.gate2_danger_met,
                gate3=req.gate3_loss_control,
                raw_data=req.model_dump() # Pass full request data for gate derivation
            )

            # Extract SHAP factors for this row
            top_factors = []
            if sv_all is not None:
                try:
                    # Logic to extract top factors for predicted class i
                    predicted_class_idx = LABEL_ORDER.index(risk["risk_level"])
                    if isinstance(sv_all, list): # Multi-class output
                        sample_shap = sv_all[predicted_class_idx][i]
                    else: # Single array (sometimes happens with specific SHAP versions/models)
                        if sv_all.ndim == 3:
                            sample_shap = sv_all[i, :, predicted_class_idx]
                        else:
                            sample_shap = sv_all[i]

                    top_indices = np.argsort(np.abs(sample_shap))[::-1][:3]
                    top_factors = [self.feat_cols[idx] for idx in top_indices]
                except Exception: pass

            features_dict = {
                "sleep_hours": req.sleep_hours, "stress_level": req.Q30_StressLevel,
                "phone_checks": req.Q11_PhoneChecks_PerHr, "missed_classes": 0,
            }

            recs = get_recommendations(risk["risk_level"], features=features_dict, top_factors=top_factors)

            results.append({
                "risk_level":         risk["risk_level"],
                "risk_score":         risk["risk_score"],
                "confidence":         risk["confidence"],
                "recommendation":     get_primary_recommendation(risk["risk_level"]),
                "recommendations":    recs["recommendations"],
                "intervention_level": recs["intervention_level"],
                "top_factors":        top_factors,
                "timestamp":          now_ts,
                "raw_prediction":     risk.get("raw_prediction"),
                "downgraded":         risk.get("downgraded", False),
                "gate_results":       risk.get("gate_results"),
                "model_version":      "1.0.0",
            })

        return results

    def _trigger_severe_alert(self, result, req):
        """Minimal webhook/alert stub for Severe risk levels."""
        print(f"\n[ALERT] SEVERE GADGET ADDICTION DETECTED")
        print(f"Risk Score: {result['risk_score']}")
        print(f"Notification sent to academic counselor (Stub)")
        # In production, this would trigger an FCM push or email/SMS.


# Module-level singleton
_service: ModelService = None


def get_service() -> ModelService:
    global _service
    if _service is None:
        _service = ModelService()
    return _service
