"""
src/preprocessor.py
Complete feature engineering: ordinal encoding, frequency mapping,
nominal encoding, scaling, missing value imputation, and pipeline saving.
"""

import os, json, joblib
import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import OrdinalEncoder, OneHotEncoder
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import (ORDINAL_MAPS, FREQ_MAPS, YESNO_MAPS, TRIED_REDUCE_MAP,
                             EXERCISE_BAND_MAP, EXERCISE_NUMERIC_MAP, MODEL_DIR, REPORT_DIR)


# ─── Column Type Registry ──────────────────────────────────────────────────────

# Columns with explicit ordinal / ordered categories
ORDINAL_COLS = list(ORDINAL_MAPS.keys())

# Columns that use Never/Rarely/…/Always scale (clean string columns)
FREQ_COLS = [
    "Q10_LateNightUsage",
    "Q12_PhantomCheckUrge",
    "Q24_GadgetBefore30minSleep",
    "Q25_WakesForPhone",
    "Q29_SkipsMeals",
]

# Columns already numeric Likert scales (1-5) - pass through as numeric
LIKERT_NUMERIC_COLS = [
    "Q13_AnxiousWithoutGadget",
    "Q18_GadgetDistractsStudy",
    "Q20_ProductivityRating",
    "Q21_GadgetHelpsAcademic",
    "Q30_StressLevel",
    "Q31_SocialInteractionQuality",
    "Q33_EmotionalCoping",
    "Q34_UsageOutOfControl",
    "Q38_SatisfactionRating",
]

# Mixed-type columns (data quality issue: contain BOTH frequency words AND
# numeric Likert codes). Cleaned via _clean_mixed_freq() -> numeric 0-5 scale.
MIXED_FREQ_COLS = [
    "Q14_GadgetAtMeals",
    "Q26_SleepQuality",
    "Q32_PrefersOnline",
]

# Yes/No / agreement columns
YESNO_COLS = [
    "Q35_FeltNeedToReduce",
    "Q36_TriedToReduce",
]

# Free-text multi-value columns (encode as count)
COUNT_COLS = ["Q8_GadgetUses", "Q28_PhysicalSymptoms"]

# Remaining categoricals → OneHot
NOMINAL_COLS = [
    "Q2_Gender", "Q4_Department", "Q5_Residence", "Q6_PrimaryGadget",
    "Q15_GadgetFreeDay", "Q22_AssignmentsOnTime", "Q37_SelfLabel",
]

# Numeric cols already numeric
NUMERIC_COLS = [
    "daily_screen_time_hours", "late_night_usage",
    "missed_classes_per_month", "sleep_hours",
    "sleep_disturbances", "social_interaction_quality",
]


def _safe_freq(val):
    """Map frequency string → int; handle NaN and partial matches."""
    if pd.isna(val):
        return np.nan
    v = str(val).strip().title()
    return FREQ_MAPS.get(v, np.nan)


def _clean_mixed_freq(val):
    """
    Handle mixed-type columns containing BOTH frequency words (Never..Always)
    AND raw numeric Likert codes (1-5) due to upstream data quality issues.
    Maps everything onto a unified 0-5 scale.
    """
    if pd.isna(val):
        return np.nan
    s = str(val).strip()
    # If it's purely numeric, treat 1-5 Likert as already on a comparable scale
    if s.replace(".", "", 1).isdigit():
        return float(s)
    v = s.title()
    if v in FREQ_MAPS:
        return float(FREQ_MAPS[v])
    if v == "Usually":
        return float(FREQ_MAPS["Often"])
    if v == "Yes Regularly":
        return float(FREQ_MAPS["Always"])
    return np.nan


def _clean_exercise(val):
    """
    Clean Q27_ExercisePerWeek: handles both band strings ("<1","1-3","3-5","5+")
    and raw numeric day-counts (as int or numeric string "1".."5"),
    mapping everything onto a unified 0-3 ordinal scale.
    """
    if pd.isna(val):
        return np.nan
    s = str(val).strip()
    if s in EXERCISE_BAND_MAP:
        return float(EXERCISE_BAND_MAP[s])
    if s.isdigit():
        n = int(s)
        return float(EXERCISE_NUMERIC_MAP.get(n, np.nan))
    return np.nan


def _safe_yesno(val, mapping):
    if pd.isna(val):
        return np.nan
    v = str(val).strip()
    return mapping.get(v, np.nan)


def _count_items(val):
    """Count comma-separated items in a cell."""
    if pd.isna(val) or val is None:
        return 0
    s = str(val).strip()
    if not s or s.lower() in ("none", "nan", "null", "none reported"):
        return 0
    return len(s.split(","))


def sanitize_prediction_input(row_dict: dict, defaults: dict) -> dict:
    """
    Sanitize and coerce incoming raw prediction input dictionary to match
    the expected schema and model training encodings.
    Only genuinely invalid or missing data should raise a ValueError.
    """
    REQUIRED_COLS = [
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
    
    sanitized = {}
    
    for col in REQUIRED_COLS:
        if col not in row_dict:
            raise ValueError(f"{col}: missing required column")
        
        val = row_dict[col]
        
        # Handle blank/null values by imputing from defaults
        if pd.isna(val) or val is None or str(val).strip() == "":
            if col == "Q28_PhysicalSymptoms":
                sanitized[col] = ""  # Treat as valid "none reported"
            else:
                default_val = defaults.get(col)
                if default_val is None:
                    raise ValueError(f"{col}: field is empty and has no default")
                sanitized[col] = default_val
            continue
            
        # Coerce based on column category
        
        # A. sleep_hours: float, range 0 to 24
        if col == "sleep_hours":
            try:
                fval = float(val)
                if not (0 <= fval <= 24):
                    raise ValueError(f"sleep_hours: must be between 0 and 24, got {fval}")
                sanitized[col] = fval
            except (TypeError, ValueError):
                raise ValueError(f"sleep_hours: couldn't interpret value '{val}' as a valid number")
            continue
            
        # B. LIKERT_NUMERIC_COLS: numeric Likert scales (1-5)
        if col in LIKERT_NUMERIC_COLS:
            try:
                fval = float(val)
                ival = int(fval)
                if not (1 <= fval <= 5):
                    raise ValueError(f"{col}: must be between 1 and 5, got {val}")
                sanitized[col] = ival if ival == fval else fval
            except (TypeError, ValueError):
                raise ValueError(f"{col}: couldn't interpret value '{val}' as a valid number between 1 and 5")
            continue
            
        # C. MIXED_FREQ_COLS: Q14_GadgetAtMeals, Q26_SleepQuality, Q32_PrefersOnline
        # Mixed frequency / numeric columns. Can be string category OR numeric.
        if col in MIXED_FREQ_COLS:
            sval = str(val).strip()
            title_val = sval.title()
            valid_freqs = {"Never", "Rarely", "Occasionally", "Sometimes", "Often", "Always", "Usually", "Yes Regularly"}
            if title_val in valid_freqs:
                sanitized[col] = title_val
            else:
                # Try to parse as float/int
                try:
                    fval = float(sval)
                    if not (0 <= fval <= 5):
                        raise ValueError(f"{col}: numeric value must be between 0 and 5, got {val}")
                    sanitized[col] = int(fval) if fval.is_integer() else fval
                except ValueError:
                    raise ValueError(f"{col}: couldn't interpret value '{val}'")
            continue
            
        # D. Q27_ExercisePerWeek: mixed str ranges and raw numeric
        if col == "Q27_ExercisePerWeek":
            sval = str(val).strip()
            if sval in EXERCISE_BAND_MAP:
                sanitized[col] = sval
            else:
                try:
                    fval = float(sval)
                    ival = int(fval)
                    if ival in EXERCISE_NUMERIC_MAP or (1 <= ival <= 5):
                        sanitized[col] = ival
                    else:
                        raise ValueError(f"Q27_ExercisePerWeek: numeric value must be in 1-5 range, got {val}")
                except ValueError:
                    raise ValueError(f"Q27_ExercisePerWeek: couldn't interpret value '{val}'")
            continue
            
        # E. ORDINAL_MAPS columns
        if col in ORDINAL_MAPS:
            sval = str(val).strip()
            # If already a valid band, use it
            if sval in ORDINAL_MAPS[col]:
                sanitized[col] = sval
            else:
                # Try to parse as numeric and map to band
                try:
                    fval = float(sval)
                    if col == "Q1_AgeGroup":
                        if fval <= 18: sanitized[col] = "15-18"
                        elif fval <= 21: sanitized[col] = "19-21"
                        elif fval <= 25: sanitized[col] = "22-25"
                        else: sanitized[col] = "25+"
                    elif col == "Q3_YearOfStudy":
                        ival = int(round(fval))
                        if ival <= 1: sanitized[col] = "1st Year"
                        elif ival == 2: sanitized[col] = "2nd Year"
                        elif ival == 3: sanitized[col] = "3rd Year"
                        else: sanitized[col] = "4th Year"
                    elif col == "Q7_ScreenTime":
                        if fval < 2: sanitized[col] = "<2"
                        elif fval < 3: sanitized[col] = "2-3"
                        elif fval < 5: sanitized[col] = "3-5"
                        elif fval < 7: sanitized[col] = "5-7"
                        elif fval < 9: sanitized[col] = "7-9"
                        else: sanitized[col] = "9+"
                    elif col == "Q11_PhoneChecks_PerHr":
                        if fval <= 2: sanitized[col] = "1-2"
                        elif fval <= 5: sanitized[col] = "3-5"
                        elif fval <= 10: sanitized[col] = "6-10"
                        else: sanitized[col] = "10+"
                    elif col == "Q16_CGPA":
                        if fval < 5: sanitized[col] = "<5"
                        elif fval < 6: sanitized[col] = "5-6"
                        elif fval < 7: sanitized[col] = "6-7"
                        elif fval < 8: sanitized[col] = "7-8"
                        elif fval < 9: sanitized[col] = "8-9"
                        else: sanitized[col] = "9-10"
                    elif col == "Q17_ClassesMissed":
                        if fval <= 2: sanitized[col] = "0-2"
                        elif fval <= 5: sanitized[col] = "3-5"
                        elif fval <= 10: sanitized[col] = "6-10"
                        else: sanitized[col] = "10+"
                    elif col == "Q19_SelfStudyHrs":
                        if fval < 1: sanitized[col] = "<1"
                        elif fval < 2: sanitized[col] = "1-2"
                        elif fval < 3: sanitized[col] = "2-3"
                        elif fval < 4: sanitized[col] = "3-4"
                        else: sanitized[col] = "4+"
                    elif col == "Q23_SleepDuration":
                        if fval < 4: sanitized[col] = "<4"
                        elif fval < 5: sanitized[col] = "4-5"
                        elif fval < 6: sanitized[col] = "5-6"
                        elif fval < 7: sanitized[col] = "6-7"
                        elif fval < 8: sanitized[col] = "7-8"
                        else: sanitized[col] = "8+"
                    elif col == "Q9_SocialPlatforms":
                        if fval <= 1: sanitized[col] = "0-1"
                        elif fval <= 3: sanitized[col] = "2-3"
                        elif fval <= 5: sanitized[col] = "4-5"
                        else: sanitized[col] = "5+"
                    else:
                        raise ValueError()
                except ValueError:
                    raise ValueError(f"{col}: couldn't interpret value '{val}'")
            continue
            
        # F. FREQ_COLS: Never/Rarely/…/Always scale (clean string columns)
        if col in FREQ_COLS:
            sval = str(val).strip()
            title_val = sval.title()
            if title_val in FREQ_MAPS:
                sanitized[col] = title_val
            elif title_val == "Usually":
                sanitized[col] = "Often"
            elif title_val == "Yes Regularly":
                sanitized[col] = "Always"
            else:
                # Try to parse as numeric
                try:
                    fval = float(sval)
                    ival = int(round(fval))
                    rev_freq = {v: k for k, v in FREQ_MAPS.items()}
                    if ival in rev_freq:
                        sanitized[col] = rev_freq[ival]
                    else:
                        raise ValueError()
                except ValueError:
                    raise ValueError(f"{col}: couldn't interpret value '{val}'")
            continue
            
        # G. YESNO_COLS: Q35_FeltNeedToReduce, Q36_TriedToReduce
        if col == "Q35_FeltNeedToReduce":
            sval = str(val).strip()
            matched = None
            for key, numeric_val in YESNO_MAPS.items():
                if key.lower() == sval.lower() or sval == str(numeric_val) or sval == f"{numeric_val}.0":
                    matched = key
                    break
            if matched:
                sanitized[col] = matched
            else:
                raise ValueError(f"Q35_FeltNeedToReduce: couldn't interpret value '{val}'")
            continue
            
        if col == "Q36_TriedToReduce":
            sval = str(val).strip()
            matched = None
            for key, numeric_val in TRIED_REDUCE_MAP.items():
                if key.lower() == sval.lower() or sval == str(numeric_val) or sval == f"{numeric_val}.0":
                    matched = key
                    break
            if matched:
                sanitized[col] = matched
            else:
                raise ValueError(f"Q36_TriedToReduce: couldn't interpret value '{val}'")
            continue
            
        # H. Count and NOMINAL columns
        sanitized[col] = str(val).strip()
        
    return sanitized



def manual_encode(X: pd.DataFrame) -> pd.DataFrame:
    """
    Apply hand-crafted encodings for ordinal, frequency, and yes/no columns
    before passing through the sklearn ColumnTransformer.
    """
    df = X.copy()

    # --- Ordinal ---
    for col, mapping in ORDINAL_MAPS.items():
        if col in df.columns:
            df[col] = df[col].map(mapping)

    # --- Exercise per week (mixed band/numeric) ---
    if "Q27_ExercisePerWeek" in df.columns:
        df["Q27_ExercisePerWeek"] = df["Q27_ExercisePerWeek"].apply(_clean_exercise)

    # --- Frequency ---
    for col in FREQ_COLS:
        if col in df.columns:
            df[col] = df[col].apply(_safe_freq)

    # --- Mixed frequency / numeric columns ---
    for col in MIXED_FREQ_COLS:
        if col in df.columns:
            df[col] = df[col].apply(_clean_mixed_freq)

    # --- Yes/No ---
    if "Q35_FeltNeedToReduce" in df.columns:
        df["Q35_FeltNeedToReduce"] = df["Q35_FeltNeedToReduce"].apply(lambda v: _safe_yesno(v, YESNO_MAPS))
    if "Q36_TriedToReduce" in df.columns:
        df["Q36_TriedToReduce"] = df["Q36_TriedToReduce"].apply(lambda v: _safe_yesno(v, TRIED_REDUCE_MAP))

    # --- Count multi-value ---
    for col in COUNT_COLS:
        if col in df.columns:
            new_name = col + "_count"
            df[new_name] = df[col].apply(_count_items)
            df.drop(columns=[col], inplace=True)

    return df


def build_pipeline(X_encoded: pd.DataFrame):
    """
    Build sklearn ColumnTransformer that handles:
    - Numeric imputation + scaling
    - Remaining categorical columns (OneHot after imputation)
    Returns fitted transformer (must be fit on training data only).
    """
    # Identify which columns are numeric after manual encoding
    num_cols = X_encoded.select_dtypes(include=np.number).columns.tolist()
    cat_cols = X_encoded.select_dtypes(exclude=np.number).columns.tolist()

    num_pipe = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler",  StandardScaler()),
    ])

    cat_pipe = Pipeline([
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("onehot",  OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
    ])

    transformers = []
    if num_cols:
        transformers.append(("num", num_pipe, num_cols))
    if cat_cols:
        transformers.append(("cat", cat_pipe, cat_cols))

    ct = ColumnTransformer(transformers=transformers, remainder="drop")
    return ct, num_cols, cat_cols


def get_feature_names(ct, num_cols, cat_cols):
    """Extract feature names from a fitted ColumnTransformer."""
    names = list(num_cols)
    if cat_cols:
        ohe = ct.named_transformers_["cat"]["onehot"]
        names += list(ohe.get_feature_names_out(cat_cols))
    return names


def fit_transform_pipeline(X_train_raw, X_test_raw):
    """
    Full pipeline: manual encode → ColumnTransformer fit/transform.
    Returns (X_train_arr, X_test_arr, ct, num_cols, cat_cols, encoded_train_df)
    """
    X_tr_enc = manual_encode(X_train_raw)
    X_te_enc = manual_encode(X_test_raw)

    ct, num_cols, cat_cols = build_pipeline(X_tr_enc)

    X_tr_arr = ct.fit_transform(X_tr_enc)
    X_te_arr = ct.transform(X_te_enc)

    feat_names = get_feature_names(ct, num_cols, cat_cols)

    return X_tr_arr, X_te_arr, ct, num_cols, cat_cols, feat_names, X_tr_enc


def save_pipeline(ct, feat_names: list, label_encoder):
    """Persist pipeline artifacts to the models directory."""
    os.makedirs(MODEL_DIR, exist_ok=True)
    joblib.dump(ct, os.path.join(MODEL_DIR, "feature_pipeline.pkl"))
    joblib.dump(label_encoder, os.path.join(MODEL_DIR, "label_encoder.pkl"))
    with open(os.path.join(MODEL_DIR, "feature_columns.json"), "w") as f:
        json.dump(feat_names, f, indent=2)
    print(f"[Preprocessor] Pipeline artifacts saved → {MODEL_DIR}")


def save_feature_report(X_raw: pd.DataFrame, X_enc: np.ndarray, feat_names: list):
    """Write feature_engineering_report.md."""
    os.makedirs(REPORT_DIR, exist_ok=True)
    path = os.path.join(REPORT_DIR, "feature_engineering_report.md")

    missing = X_raw.isnull().sum()
    missing = missing[missing > 0]
    dup_count = X_raw.duplicated().sum()

    lines = [
        "# Feature Engineering Report\n\n",
        f"## Dataset Dimensions\n",
        f"- Raw features (before encoding): **{X_raw.shape[1]}**\n",
        f"- Encoded feature dimensions: **{X_enc.shape[1]}**\n\n",
        "## Missing Values\n\n",
    ]
    if missing.empty:
        lines.append("No missing values detected after encoding.\n\n")
    else:
        lines.append("| Column | Missing Count |\n|--------|---------------|\n")
        for col, cnt in missing.items():
            lines.append(f"| `{col}` | {cnt} |\n")
        lines.append("\nMissing values imputed using **median** (numeric) and **most_frequent** (categorical).\n\n")

    lines += [
        f"## Duplicate Records\n",
        f"Duplicate rows detected: **{dup_count}**\n",
        "(Duplicates kept because dataset is small; they may represent genuine repeated survey responses.)\n\n",
        "## Encoding Strategy\n\n",
        "| Type | Strategy |\n|------|----------|\n",
        "| Ordered categoricals (ScreenTime, CGPA, etc.) | Ordinal integer mapping |\n",
        "| Frequency responses (Never→Always) | Integer 0–5 |\n",
        "| Yes/No agreement responses | Integer 0–2 |\n",
        "| Multi-value text (GadgetUses, Symptoms) | Item count |\n",
        "| Nominal (Gender, Department) | One-Hot Encoding |\n",
        "| Numeric | StandardScaler (mean=0, std=1) |\n\n",
        "## Feature Selection\n\n",
        "Features with near-zero variance are removed by the tree models automatically. "
        "SHAP importance analysis performed post-training.\n\n",
        "## Outlier Treatment\n\n",
        "Outliers are retained. StandardScaler clips extreme values implicitly via z-score "
        "normalization. Tree models are inherently robust to outliers.\n",
    ]

    with open(path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    print(f"[Preprocessor] Feature engineering report saved → {path}")
