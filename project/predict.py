"""
predict.py
Standalone CLI prediction script.
Usage: python predict.py --screen_time "7-9" --sleep_hours 5.5 --stress_level 4 --phone_checks 10
"""

import argparse, sys, os, json
import numpy as np
import pandas as pd
import joblib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from configs.config        import MODEL_DIR, LABEL_ORDER
from src.preprocessor      import manual_encode
from src.feature_derivation import build_full_record
from risk_engine           import proba_to_risk
from recommendation_engine import get_recommendations


def load_artifacts():
    model    = joblib.load(os.path.join(MODEL_DIR, "best_model.pkl"))
    pipeline = joblib.load(os.path.join(MODEL_DIR, "feature_pipeline.pkl"))
    with open(os.path.join(MODEL_DIR, "feature_columns.json")) as f:
        feat_cols = json.load(f)
    with open(os.path.join(MODEL_DIR, "default_inputs.json")) as f:
        defaults = json.load(f)
    return model, pipeline, feat_cols, defaults


def predict_single(input_dict: dict) -> dict:
    """
    Run a single prediction from a (possibly partial) input dictionary.
    Missing fields are filled with dataset-derived defaults (median/mode).
    """
    model, pipeline, feat_cols, defaults = load_artifacts()

    # Merge user inputs with derived correlated fields and dataset defaults
    full_input = build_full_record(input_dict, defaults)

    df = pd.DataFrame([full_input])
    df_enc = manual_encode(df)

    X = pipeline.transform(df_enc)

    proba = model.predict_proba(X)[0]

    result = proba_to_risk(proba, LABEL_ORDER)
    recs   = get_recommendations(result["risk_level"], features={
        "sleep_hours":       input_dict.get("sleep_hours", 7),
        "screen_time_hours": input_dict.get("daily_screen_time_hours", 4),
        "stress_level":      input_dict.get("Q30_StressLevel", 2),
        "phone_checks":      input_dict.get("Q11_PhoneChecks_PerHr", 2),
        "missed_classes":    input_dict.get("missed_classes_per_month", 0),
    })
    result["recommendations"] = recs["recommendations"]
    result["intervention_level"] = recs["intervention_level"]
    return result


def main():
    parser = argparse.ArgumentParser(description="Gadget Addiction Risk Prediction")
    parser.add_argument("--screen_time",      default="3-5",   help="Q7_ScreenTime category")
    parser.add_argument("--sleep_hours",       type=float, default=7.0)
    parser.add_argument("--stress_level",      type=int,   default=2, help="1-5")
    parser.add_argument("--phone_checks",      default="3-5",  help="Q11_PhoneChecks_PerHr")
    parser.add_argument("--late_night",        default="Sometimes")
    args = parser.parse_args()

    input_data = {
        "Q7_ScreenTime":        args.screen_time,
        "sleep_hours":          args.sleep_hours,
        "Q30_StressLevel":      args.stress_level,
        "Q11_PhoneChecks_PerHr": args.phone_checks,
        "Q10_LateNightUsage":   args.late_night,
    }

    print("\n[Predict] Input:", input_data)
    result = predict_single(input_data)
    print("\n[Predict] Result:")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
