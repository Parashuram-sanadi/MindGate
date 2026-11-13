"""
Central configuration for Electronic Gadget Addiction Prediction System.
"""

import os

# ─── Paths ────────────────────────────────────────────────────────────────────
BASE_DIR    = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR    = os.path.join(BASE_DIR, "data")
MODEL_DIR   = os.path.join(BASE_DIR, "models")
REPORT_DIR  = os.path.join(BASE_DIR, "reports")
PLOT_DIR    = os.path.join(BASE_DIR, "plots")

DATASET_FILE = os.path.join(DATA_DIR, "augmented_dataset_final.xlsx")

# ─── Target ───────────────────────────────────────────────────────────────────
TARGET_COL     = "Corrected_Label"
LABEL_ORDER    = ["Low", "Moderate", "High", "Severe"]

# ─── Risk Score Bands ─────────────────────────────────────────────────────────
RISK_BANDS = {
    "Low":      (0,  34),
    "Moderate": (35, 54),
    "High":     (55, 64),
    "Severe":   (65, 999),
}

# ─── Leakage Columns (NEVER used in training) ─────────────────────────────────
LEAKAGE_COLS = [
    "Predicted_AddictionRisk",
    "computed_risk_score",
    "score_usage_compulsion",
    "score_sleep_physical",
    "score_academic_impairment",
    "score_emotional_dependence",
    "score_social_withdrawal",
    "score_loss_of_control",
    "danger_marker_count",
    "gate1_score_valid",
    "gate2_danger_met",
    "gate3_loss_control",
    "all_gates_pass",
]

# ─── Meta / ID Columns ────────────────────────────────────────────────────────
META_COLS = ["Name", "ID", "Scenario_Note", "Q35_encoded", "Q36_encoded", "Q10_encoded"]

# ─── Training Settings ────────────────────────────────────────────────────────
TEST_SIZE    = 0.20
RANDOM_STATE = 42
CV_FOLDS     = 5

# ─── Optuna ───────────────────────────────────────────────────────────────────
OPTUNA_TRIALS = 15

# ─── Ordered Categorical Maps ─────────────────────────────────────────────────
ORDINAL_MAPS = {
    "Q1_AgeGroup":        {"15-18": 0, "19-21": 1, "22-25": 2, "25+": 3},
    "Q3_YearOfStudy":     {"1st Year": 0, "2nd Year": 1, "3rd Year": 2, "4th Year": 3},
    "Q7_ScreenTime":      {"<2": 0, "2-3": 1, "3-5": 2, "5-7": 3, "7-9": 4, "9+": 5},
    "Q11_PhoneChecks_PerHr": {"1-2": 0, "3-5": 1, "6-10": 2, "10+": 3},
    "Q16_CGPA":           {"<5": 0, "5-6": 1, "6-7": 2, "7-8": 3, "8-9": 4, "9-10": 5},
    "Q17_ClassesMissed":  {"0-2": 0, "3-5": 1, "6-10": 2, "10+": 3},
    "Q19_SelfStudyHrs":   {"<1": 0, "1-2": 1, "2-3": 2, "3-4": 3, "4+": 4},
    "Q23_SleepDuration":  {"<4": 0, "4-5": 1, "5-6": 2, "6-7": 3, "7-8": 4, "8+": 5},
    "Q9_SocialPlatforms": {"0-1": 0, "2-3": 1, "4-5": 2, "5+": 3},
}

# Q27_ExercisePerWeek has a DATA QUALITY ISSUE: mixes banded strings
# ("<1","1-3","3-5","5+") with raw numeric day-counts (1-5) as strings/ints.
# Mapped onto a unified 0-4 ordinal scale by _clean_exercise() in preprocessor.
EXERCISE_BAND_MAP = {"<1": 0, "1-3": 1, "3-5": 2, "5+": 3}
EXERCISE_NUMERIC_MAP = {1: 0, 2: 1, 3: 1, 4: 2, 5: 3}   # approximate band equivalent

FREQ_MAPS = {
    "Never":        0,
    "Rarely":       1,
    "Occasionally": 2,
    "Sometimes":    3,
    "Often":        4,
    "Always":       5,
}

YESNO_MAPS = {
    "Not really":          0,
    "Yes sometimes":       1,
    "Yes often":           2,
}

TRIED_REDUCE_MAP = {
    "Never tried":         0,
    "Tried and failed":    1,
    "Tried and succeeded": 2,
}
