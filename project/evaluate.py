"""
evaluate.py
Post-training evaluation: loads best model, generates classification reports,
confusion matrices, and ROC plots.

Run: python evaluate.py
"""

import os, sys, json, warnings
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns
import joblib
warnings.filterwarnings("ignore")

from sklearn.model_selection import train_test_split
from sklearn.metrics         import (
    classification_report, confusion_matrix,
    balanced_accuracy_score, roc_auc_score,
    RocCurveDisplay
)
from sklearn.preprocessing   import label_binarize

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from configs.config      import (RANDOM_STATE, TEST_SIZE, LABEL_ORDER,
                                   MODEL_DIR, REPORT_DIR, PLOT_DIR)
from src.data_loader     import load_dataset
from src.preprocessor    import fit_transform_pipeline


def load_artifacts():
    model    = joblib.load(os.path.join(MODEL_DIR, "best_model.pkl"))
    pipeline = joblib.load(os.path.join(MODEL_DIR, "feature_pipeline.pkl"))
    with open(os.path.join(MODEL_DIR, "risk_mapping.json")) as f:
        meta = json.load(f)
    return model, pipeline, meta


def main():
    print("[Evaluate] Loading artifacts ...")
    model, pipeline, meta = load_artifacts()
    model_name = meta.get("best_model_name", "Best Model")

    print("[Evaluate] Loading dataset ...")
    X_raw, y, gate_df, _ = load_dataset()

    # Reconstruct same split used in training
    X_tr_raw, X_te_raw, y_tr, y_te = train_test_split(
        X_raw, y, test_size=TEST_SIZE, random_state=RANDOM_STATE, stratify=y)

    _, X_te_arr, *_ = fit_transform_pipeline(X_tr_raw, X_te_raw)

    y_pred = model.predict(X_te_arr)
    y_prob = model.predict_proba(X_te_arr) if hasattr(model, "predict_proba") else None

    # ── Classification Report ─────────────────────────────────────────────────
    report = classification_report(y_te, y_pred, target_names=LABEL_ORDER)
    print("\n[Evaluate] Classification Report:\n")
    print(report)

    os.makedirs(REPORT_DIR, exist_ok=True)
    with open(os.path.join(REPORT_DIR, "classification_report.txt"), "w") as f:
        f.write(f"Model: {model_name}\n\n")
        f.write(report)
    print(f"[Evaluate] Report saved → {REPORT_DIR}/classification_report.txt")

    # ── Balanced Accuracy ────────────────────────────────────────────────────
    ba = balanced_accuracy_score(y_te, y_pred)
    print(f"[Evaluate] Balanced Accuracy: {ba:.4f}")

    # ── Confusion Matrix Plot ─────────────────────────────────────────────────
    cm = confusion_matrix(y_te, y_pred)
    fig, ax = plt.subplots(figsize=(7, 6))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=LABEL_ORDER, yticklabels=LABEL_ORDER, ax=ax)
    ax.set_title(f"Confusion Matrix – {model_name}", fontsize=13, fontweight="bold")
    ax.set_ylabel("Actual")
    ax.set_xlabel("Predicted")
    plt.tight_layout()
    os.makedirs(PLOT_DIR, exist_ok=True)
    path = os.path.join(PLOT_DIR, "confusion_matrix_best_model.png")
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"[Evaluate] Confusion matrix saved → {path}")

    # ── ROC AUC (one-vs-rest) ─────────────────────────────────────────────────
    if y_prob is not None:
        y_te_bin = label_binarize(y_te, classes=list(range(len(LABEL_ORDER))))
        try:
            auc = roc_auc_score(y_te_bin, y_prob, multi_class="ovr", average="macro")
            print(f"[Evaluate] ROC AUC (macro OvR): {auc:.4f}")

            fig, ax = plt.subplots(figsize=(8, 6))
            for i, label in enumerate(LABEL_ORDER):
                if i < y_te_bin.shape[1] and i < y_prob.shape[1]:
                    RocCurveDisplay.from_predictions(
                        y_te_bin[:, i], y_prob[:, i],
                        name=label, ax=ax
                    )
            ax.set_title(f"ROC Curves (OvR) – {model_name}", fontsize=13, fontweight="bold")
            ax.plot([0,1],[0,1], "k--", alpha=0.5)
            plt.tight_layout()
            roc_path = os.path.join(PLOT_DIR, "roc_curves_best_model.png")
            fig.savefig(roc_path, dpi=150, bbox_inches="tight")
            plt.close(fig)
            print(f"[Evaluate] ROC curves saved → {roc_path}")
        except Exception as e:
            print(f"[Evaluate] ROC AUC skipped: {e}")

    print("\n[Evaluate] ✅ Evaluation complete.")


if __name__ == "__main__":
    main()
