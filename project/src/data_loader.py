"""
src/data_loader.py
Loads dataset, separates features/target, removes leakage columns,
and produces a leakage report.
"""

import os
import json
import pandas as pd
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import (
    DATASET_FILE, TARGET_COL, LEAKAGE_COLS, META_COLS,
    REPORT_DIR, LABEL_ORDER
)


def load_raw() -> pd.DataFrame:
    """Load the raw Excel dataset."""
    df = pd.read_excel(DATASET_FILE)
    print(f"[DataLoader] Loaded {df.shape[0]} rows × {df.shape[1]} columns.")
    return df


def check_leakage(df: pd.DataFrame) -> dict:
    """
    Detect which leakage columns are present in the dataset.
    Returns a report dict.
    """
    present   = [c for c in LEAKAGE_COLS if c in df.columns]
    absent    = [c for c in LEAKAGE_COLS if c not in df.columns]
    report = {
        "leakage_columns_found":   present,
        "leakage_columns_missing": absent,
        "total_leakage_found":     len(present),
        "action": "All leakage columns removed before training.",
    }
    return report


def save_leakage_report(report: dict):
    """Write leakage_report.md to the reports directory."""
    os.makedirs(REPORT_DIR, exist_ok=True)
    path = os.path.join(REPORT_DIR, "leakage_report.md")
    lines = [
        "# Data Leakage Prevention Report\n",
        "## Leakage Columns Found in Dataset\n",
        "The following columns were identified as potential data leakage sources "
        "and have been **removed** from the training feature set:\n\n",
    ]
    for c in report["leakage_columns_found"]:
        lines.append(f"- `{c}`\n")
    lines += [
        f"\n**Total leakage columns removed:** {report['total_leakage_found']}\n\n",
        "## Why These Columns Are Leakage\n\n",
        "These columns are **derived from the target** (computed risk score, gate logic, "
        "sub-scores). Using them during training would inflate model performance and make "
        "the model useless in production where these values are unavailable at inference "
        "time.\n\n",
        "## Columns Permitted ONLY For Post-Processing\n\n",
        "| Column | Permitted Use |\n",
        "|--------|---------------|\n",
        "| `computed_risk_score` | Risk band validation, severity downgrade logic |\n",
        "| `gate1_score_valid` | Severe risk verification |\n",
        "| `gate2_danger_met` | Severe risk verification |\n",
        "| `gate3_loss_control` | Severe risk verification |\n",
        "| `all_gates_pass` | Final severity confirmation |\n",
        "| `Predicted_AddictionRisk` | Evaluation / comparison only |\n\n",
        "## Action Taken\n\n",
        "All leakage columns stripped from `X_train`, `X_val`, and `X_test` "
        "before any model sees the data.\n",
    ]
    with open(path, "w") as f:
        f.writelines(lines)
    print(f"[DataLoader] Leakage report saved → {path}")


def get_feature_target(df: pd.DataFrame):
    """
    Strip leakage + meta columns.
    Returns (X, y, gate_cols_df).
    """
    # Save gate columns for post-processing
    gate_cols = [c for c in LEAKAGE_COLS if c in df.columns]
    gate_df   = df[gate_cols].copy()

    drop_cols = META_COLS + LEAKAGE_COLS + [TARGET_COL]
    drop_cols = [c for c in drop_cols if c in df.columns]

    X = df.drop(columns=drop_cols)
    y = df[TARGET_COL].copy()

    # Encode target as integers
    y = pd.Categorical(y, categories=LABEL_ORDER, ordered=True).codes
    return X, y, gate_df


def load_dataset():
    """Full pipeline: load → leakage check → split X/y."""
    df       = load_raw()
    report   = check_leakage(df)
    save_leakage_report(report)
    X, y, gate_df = get_feature_target(df)
    print(f"[DataLoader] Features: {X.shape[1]}  |  Target classes: {len(set(y))}")
    return X, y, gate_df, df
