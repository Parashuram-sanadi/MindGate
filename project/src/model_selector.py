"""
src/model_selector.py
Ranks all trained models and selects the best automatically.
Generates leaderboard.csv and best_model_report.md.
"""

import os, sys, json
import pandas as pd
import joblib

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import MODEL_DIR, REPORT_DIR, LABEL_ORDER


def rank_models(cv_df: pd.DataFrame, test_df: pd.DataFrame) -> pd.DataFrame:
    """
    Merge CV and test results; apply 4-key ranking:
      1. CV F1 Mean (primary)
      2. CV Accuracy Mean
      3. Test F1
      4. Test Accuracy
    """
    merged = cv_df.merge(test_df, on="Model", how="outer")
    merged = merged.sort_values(
        by=["CV_F1_Mean", "CV_Accuracy_Mean", "Test_F1", "Test_Accuracy"],
        ascending=False
    ).reset_index(drop=True)
    merged.index = merged.index + 1
    merged.index.name = "Rank"

    os.makedirs(REPORT_DIR, exist_ok=True)
    leaderboard_path = os.path.join(REPORT_DIR, "leaderboard.csv")
    merged.to_csv(leaderboard_path)
    print(f"[ModelSelector] Leaderboard saved → {leaderboard_path}")
    return merged


def select_best(leaderboard: pd.DataFrame, trained_models: dict) -> tuple:
    """Return (best_model_name, best_model_object)."""
    best_name = leaderboard.iloc[0]["Model"]
    best_model = trained_models[best_name]
    print(f"[ModelSelector] ✅ Best model selected: {best_name}")
    return best_name, best_model


def save_best_model(model, model_name: str):
    os.makedirs(MODEL_DIR, exist_ok=True)
    path = os.path.join(MODEL_DIR, "best_model.pkl")
    joblib.dump(model, path)
    print(f"[ModelSelector] Best model saved → {path}")

    meta = {"best_model_name": model_name, "label_order": LABEL_ORDER}
    with open(os.path.join(MODEL_DIR, "risk_mapping.json"), "w") as f:
        json.dump(meta, f, indent=2)


def generate_best_model_report(leaderboard: pd.DataFrame, best_name: str,
                                priority_report: dict = None):
    """Write best_model_report.md."""
    row = leaderboard[leaderboard["Model"] == best_name].iloc[0]

    lines = [
        "# Best Model Report\n\n",
        f"## Selected Model: **{best_name}**\n\n",
        "### Selection Criteria\n",
        "Models were ranked by:\n",
        "1. Cross-Validation Macro F1 (primary)\n",
        "2. Cross-Validation Accuracy\n",
        "3. Test Macro F1\n",
        "4. Test Accuracy\n\n",
        "### Performance Summary\n\n",
        "| Metric | Value |\n|--------|-------|\n",
        f"| CV F1 Mean | {row.get('CV_F1_Mean', 'N/A')} |\n",
        f"| CV F1 Std  | {row.get('CV_F1_Std', 'N/A')} |\n",
        f"| CV Accuracy| {row.get('CV_Accuracy_Mean', 'N/A')} |\n",
        f"| Test F1    | {row.get('Test_F1', 'N/A')} |\n",
        f"| Test Acc   | {row.get('Test_Accuracy', 'N/A')} |\n",
        f"| Balanced Acc| {row.get('Balanced_Acc', 'N/A')} |\n",
        f"| Train Time | {row.get('Train_Time_s', 'N/A')} s |\n\n",
        "### Why This Model?\n\n",
        f"**{best_name}** was selected because it achieved the highest "
        "cross-validation F1 score among all trained models, indicating "
        "it generalises best to unseen data on this survey-based dataset.\n\n",
        "For gadget addiction prediction specifically:\n\n",
        "- The dataset contains predominantly **ordinal and categorical survey features**.\n",
        "- Gradient-boosted tree ensembles handle heterogeneous feature types robustly.\n",
        "- **CatBoost** and **Random Forest** are especially well-suited because:\n",
        "  - They require minimal preprocessing of categorical variables.\n",
        "  - They are robust to small datasets (~396 samples).\n",
        "  - They naturally handle class imbalance via `class_weight`.\n",
        "  - Their predictions are interpretable via SHAP.\n\n",
        "### Model Leaderboard (Top 5)\n\n",
        leaderboard.head(5)[["Model", "CV_F1_Mean", "CV_Accuracy_Mean",
                              "Test_F1", "Test_Accuracy"]].to_markdown(index=True),
        "\n",
    ]

    path = os.path.join(REPORT_DIR, "best_model_report.md")
    with open(path, "w") as f:
        f.writelines(lines)
    print(f"[ModelSelector] Best model report saved → {path}")
