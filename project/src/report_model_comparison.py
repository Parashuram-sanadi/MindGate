"""
src/report_model_comparison.py
Standalone model comparison and report generator for final year project report.

Does NOT modify or overwrite any production model files, thresholds, or API code.
Generates terminal tables, classification report, confusion matrix, feature importance,
and SHAP summary plots in a NEW directory: project/report_outputs/

Run:
  python -m src.report_model_comparison
"""

import os, sys, time, warnings
import numpy as np
import pandas as pd
import joblib
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

warnings.filterwarnings("ignore")

# Force UTF-8 stdout encoding for clean terminal output
if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from configs.config import RANDOM_STATE, TEST_SIZE, CV_FOLDS, LABEL_ORDER, MODEL_DIR
from src.data_loader import load_dataset
from src.preprocessor import fit_transform_pipeline
from src.model_trainer import get_base_models
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_validate
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    roc_auc_score, confusion_matrix, classification_report
)

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "report_outputs")


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    print("=" * 80)
    print("      MINDGATE MODEL COMPARISON & REPORT GENERATOR (FOR FINAL REPORT)")
    print("=" * 80)

    # 1. Load Data
    print("\n[1/6] Loading dataset and building train/test split...")
    X_raw, y, gate_df, df_full = load_dataset()

    X_tr_raw, X_te_raw, y_tr, y_te = train_test_split(
        X_raw, y, test_size=TEST_SIZE, random_state=RANDOM_STATE, stratify=y
    )

    # 2. Preprocess
    X_tr_arr, X_te_arr, ct, num_cols, cat_cols, feat_names, X_tr_enc = \
        fit_transform_pipeline(X_tr_raw, X_te_raw)

    print(f"      Train samples: {len(y_tr)} | Test samples: {len(y_te)} | Features: {X_tr_arr.shape[1]}")

    # 3. Model Training & Evaluation
    print("\n[2/6] Evaluating candidate models (5-Fold Stratified CV + Held-Out Test Set)...")
    base_models = get_base_models(len(LABEL_ORDER))

    skf = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=RANDOM_STATE)

    records = []
    trained_models = {}

    for name, model in base_models.items():
        t0 = time.time()
        # 5-fold CV
        cv = cross_validate(
            model, X_tr_arr, y_tr, cv=skf,
            scoring={"accuracy": "accuracy", "precision": "precision_macro",
                     "recall": "recall_macro", "f1": "f1_macro", "roc_auc": "roc_auc_ovr"},
            n_jobs=1
        )

        # Fit on full training set for test evaluation
        model.fit(X_tr_arr, y_tr)
        train_time = round(time.time() - t0, 3)

        y_pred = model.predict(X_te_arr)
        y_prob = model.predict_proba(X_te_arr) if hasattr(model, "predict_proba") else None

        acc = accuracy_score(y_te, y_pred)
        prec = precision_score(y_te, y_pred, average="macro", zero_division=0)
        rec = recall_score(y_te, y_pred, average="macro", zero_division=0)
        f1 = f1_score(y_te, y_pred, average="macro", zero_division=0)

        roc_auc = 0.0
        if y_prob is not None:
            try:
                roc_auc = roc_auc_score(y_te, y_prob, multi_class="ovr", average="macro")
            except Exception:
                roc_auc = 0.0

        records.append({
            "Model": name,
            "Accuracy": round(acc, 4),
            "Precision": round(prec, 4),
            "Recall": round(rec, 4),
            "F1": round(f1, 4),
            "ROC_AUC": round(roc_auc, 4),
            "CV_F1_Mean": round(cv["test_f1"].mean(), 4),
            "CV_F1_Std": round(cv["test_f1"].std(), 4),
            "Train_Time_s": train_time
        })
        trained_models[name] = model

    df_res = pd.DataFrame(records)
    # Sort by F1 descending
    df_res = df_res.sort_values(by=["F1", "CV_F1_Mean"], ascending=False).reset_index(drop=True)

    # Selected production model name
    selected_model_name = "CatBoost"

    # 4. Print Clean Fixed-Width Terminal Table
    print("\n[3/6] MODEL PERFORMANCE COMPARISON TABLE:")
    print("-" * 88)
    header = f"{'Rank':<5} {'Model':<22} {'Accuracy':<10} {'Precision':<10} {'Recall':<9} {'F1 Score':<10} {'ROC-AUC':<9} {'Time(s)':<8} {'Status'}"
    print(header)
    print("-" * 88)

    for rank, row in df_res.iterrows():
        is_selected = row["Model"] == selected_model_name
        status_tag = "<-- SELECTED" if is_selected else ""
        m_name = row["Model"]
        row_str = f"{rank+1:<5} {m_name:<22} {row['Accuracy']:<10.4f} {row['Precision']:<10.4f} {row['Recall']:<9.4f} {row['F1']:<10.4f} {row['ROC_AUC']:<9.4f} {row['Train_Time_s']:<8.3f} {status_tag}"
        print(row_str)

    print("-" * 88)

    # Save CSV
    csv_path = os.path.join(OUTPUT_DIR, "model_comparison.csv")
    df_res.to_csv(csv_path, index=False)
    print(f"\n      Saved comparison table → {csv_path}")

    # 5. Selected Model Deep Dive (CatBoost)
    print(f"\n[4/6] Deep Dive for Selected Production Model ({selected_model_name}):")
    best_model = trained_models[selected_model_name]
    y_pred_best = best_model.predict(X_te_arr)

    print("\n      Classification Report:")
    clf_rep = classification_report(y_te, y_pred_best, target_names=LABEL_ORDER)
    print(clf_rep)

    cm = confusion_matrix(y_te, y_pred_best)
    print("      Confusion Matrix:")
    print("      " + " ".join([f"{l:>10}" for l in LABEL_ORDER]))
    for idx, row in enumerate(cm):
        print(f"      {LABEL_ORDER[idx]:<10}" + " ".join([f"{v:>10}" for v in row]))

    # Plot Confusion Matrix
    fig, ax = plt.subplots(figsize=(7, 6))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=LABEL_ORDER, yticklabels=LABEL_ORDER, ax=ax)
    ax.set_title(f"Confusion Matrix – {selected_model_name} (Selected Model)", fontsize=13, fontweight="bold")
    ax.set_ylabel("Actual Risk Level")
    ax.set_xlabel("Predicted Risk Level")
    plt.tight_layout()
    cm_path = os.path.join(OUTPUT_DIR, "confusion_matrix.png")
    fig.savefig(cm_path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"\n      Saved confusion matrix plot → {cm_path}")

    # 6. Plot Bar Chart Comparison
    print("\n[5/6] Generating Model Comparison Bar Chart...")
    fig, ax = plt.subplots(figsize=(11, 6))
    y_pos = np.arange(len(df_res))
    bars = ax.barh(y_pos, df_res["F1"], color="#3498DB", height=0.6, label="F1 Score")

    # Highlight selected model
    for idx, row in df_res.iterrows():
        if row["Model"] == selected_model_name:
            bars[idx].set_color("#2ECC71")

    ax.set_yticks(y_pos)
    ax.set_yticklabels(df_res["Model"], fontsize=11)
    ax.invert_yaxis()
    ax.set_xlabel("Macro F1 Score", fontsize=12, fontweight="bold")
    ax.set_title("Model Comparison – Macro F1 Score (CatBoost Selected)", fontsize=14, fontweight="bold")

    for idx, row in df_res.iterrows():
        val = row["F1"]
        ax.text(val + 0.01, idx, f"{val:.4f}", va="center", fontsize=10, fontweight="bold")

    ax.set_xlim(0, 1.1)
    plt.tight_layout()
    bar_path = os.path.join(OUTPUT_DIR, "model_comparison.png")
    fig.savefig(bar_path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"      Saved model comparison bar chart → {bar_path}")

    # 7. Top 10 Feature Importances Plot
    print("\n[6/6] Generating Top 10 Feature Importances & SHAP Summary Plot...")
    if hasattr(best_model, "feature_importances_"):
        importances = best_model.feature_importances_
        fi_df = pd.DataFrame({"Feature": feat_names, "Importance": importances})
        fi_df = fi_df.sort_values(by="Importance", ascending=False).head(10)

        print("\n      Top 10 Most Important Features:")
        for r_idx, (_, f_row) in enumerate(fi_df.iterrows(), 1):
            print(f"      {r_idx:>2}. {f_row['Feature']:<30} {f_row['Importance']:.4f}")

        fig, ax = plt.subplots(figsize=(10, 6))
        ax.barh(fi_df["Feature"][::-1], fi_df["Importance"][::-1], color="#2ECC71", edgecolor="white")
        ax.set_title(f"Top 10 Feature Importances – {selected_model_name}", fontsize=13, fontweight="bold")
        ax.set_xlabel("Feature Importance Score")
        plt.tight_layout()
        fi_path = os.path.join(OUTPUT_DIR, "feature_importance.png")
        fig.savefig(fi_path, dpi=150, bbox_inches="tight")
        plt.close(fig)
        print(f"      Saved feature importance plot → {fi_path}")

    # SHAP Summary Plot
    try:
        import shap
        explainer = shap.TreeExplainer(best_model)
        shap_values = explainer.shap_values(X_te_arr)

        if isinstance(shap_values, list):
            sv_target = shap_values[LABEL_ORDER.index("Severe")] if len(shap_values) > 3 else shap_values[-1]
        elif isinstance(shap_values, np.ndarray) and shap_values.ndim == 3:
            sv_target = shap_values[:, :, LABEL_ORDER.index("Severe")]
        else:
            sv_target = shap_values

        fig, ax = plt.subplots(figsize=(10, 7))
        shap.summary_plot(sv_target, X_te_arr, feature_names=feat_names, show=False, max_display=12)
        plt.title(f"SHAP Summary Plot – {selected_model_name} (Severe Risk)", fontsize=13, fontweight="bold")
        plt.tight_layout()
        shap_path = os.path.join(OUTPUT_DIR, "shap_summary.png")
        fig.savefig(shap_path, dpi=150, bbox_inches="tight")
        plt.close(fig)
        print(f"      Saved SHAP summary plot → {shap_path}")
    except Exception as e:
        print(f"      SHAP plot skipped: {e}")

    print("\n" + "=" * 80)
    print(f" ✅ REPORT GENERATION COMPLETE! All outputs saved to: {OUTPUT_DIR}")
    print("=" * 80 + "\n")


if __name__ == "__main__":
    main()
