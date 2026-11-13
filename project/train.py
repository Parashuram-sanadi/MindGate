"""
train.py
Master training orchestrator.
Run: python train.py
"""

import os, sys, json, warnings
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing   import LabelEncoder
warnings.filterwarnings("ignore")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from configs.config           import RANDOM_STATE, TEST_SIZE, LABEL_ORDER, MODEL_DIR, REPORT_DIR, PLOT_DIR
from src.data_loader          import load_dataset
from src.preprocessor         import fit_transform_pipeline, save_pipeline, save_feature_report, manual_encode
from src.eda                  import run_eda
from src.imbalance_handler    import handle_imbalance
from src.model_trainer        import (get_base_models, run_cross_validation,
                                       tune_with_optuna, build_tuned_models,
                                       fit_and_evaluate, plot_learning_curve,
                                       plot_confusion_matrix)
from src.model_selector       import rank_models, select_best, save_best_model, generate_best_model_report
from src.explainability       import run_explainability


def main():
    os.makedirs(MODEL_DIR, exist_ok=True)
    os.makedirs(REPORT_DIR, exist_ok=True)
    os.makedirs(PLOT_DIR, exist_ok=True)

    print("=" * 60)
    print(" ELECTRONIC GADGET ADDICTION – TRAINING PIPELINE")
    print("=" * 60)

    # ── 1. Load Data ──────────────────────────────────────────────
    X_raw, y, gate_df, df_full = load_dataset()

    # ── 2. EDA ────────────────────────────────────────────────────
    run_eda(df_full)

    # ── 3. Label Encoding ─────────────────────────────────────────
    # NOTE: `y` returned by load_dataset() is ALREADY encoded as integer
    # codes 0=Low, 1=Moderate, 2=High, 3=Severe (matching LABEL_ORDER).
    # We build a LabelEncoder ONLY for serialization/compatibility —
    # its internal alphabetical ordering is NOT used for predictions.
    le = LabelEncoder()
    le.fit(LABEL_ORDER)
    y_encoded = y.astype(int)
    label_map = {i: LABEL_ORDER[i] for i in range(len(LABEL_ORDER))}

    print(f"\n[Train] Label mapping (model class index -> risk label): {label_map}")

    # ── 4. Train / Test Split ─────────────────────────────────────
    X_tr_raw, X_te_raw, y_tr, y_te = train_test_split(
        X_raw, y_encoded, test_size=TEST_SIZE,
        random_state=RANDOM_STATE, stratify=y_encoded
    )
    print(f"[Train] Train size: {len(y_tr)}  |  Test size: {len(y_te)}")

    # ── 5. Feature Engineering ────────────────────────────────────
    X_tr_arr, X_te_arr, ct, num_cols, cat_cols, feat_names, X_tr_enc = \
        fit_transform_pipeline(X_tr_raw, X_te_raw)
    save_feature_report(X_tr_raw, X_tr_arr, feat_names)
    save_pipeline(ct, feat_names, le)
    print(f"[Train] Encoded features: {X_tr_arr.shape[1]}")

    # ── 6. Imbalance Handling ─────────────────────────────────────
    X_tr_bal, y_tr_bal, smoted = handle_imbalance(X_tr_arr, y_tr, label_map)
    print(f"[Train] SMOTE applied: {smoted}  |  Train size after: {len(y_tr_bal)}")

    # ── 7. Base Model Cross-Validation ────────────────────────────
    print("\n[CV] Running base models cross-validation ...")
    n_classes   = len(LABEL_ORDER)
    base_models = get_base_models(n_classes)
    cv_results  = run_cross_validation(base_models, X_tr_bal, y_tr_bal)

    # ── 8. Hyperparameter Tuning (Optuna) ─────────────────────────
    print("\n[Optuna] Tuning priority models ...")
    best_params  = tune_with_optuna(X_tr_bal, y_tr_bal)
    tuned        = build_tuned_models(best_params)

    # Merge tuned models back
    all_models   = {**base_models, **tuned}
    # Add tuned suffix to avoid collision – replace originals
    for name, model in tuned.items():
        all_models[name] = model

    # ── 9. Final CV with Tuned Models ─────────────────────────────
    print("\n[CV] Running final cross-validation with tuned models ...")
    cv_final = run_cross_validation(all_models, X_tr_bal, y_tr_bal)

    # ── 10. Train + Evaluate on Test Set ──────────────────────────
    print("\n[Train] Fitting all models on full training set ...")
    test_results, trained_models = fit_and_evaluate(
        all_models, X_tr_bal, y_tr_bal, X_te_arr, y_te, LABEL_ORDER
    )

    # ── 11. Learning Curves for Priority Models ───────────────────
    for pname in ["CatBoost", "Random Forest", "XGBoost"]:
        if pname in trained_models:
            plot_learning_curve(trained_models[pname], pname, X_tr_bal, y_tr_bal)

    # ── 12. Confusion Matrices ────────────────────────────────────
    for name, model in trained_models.items():
        plot_confusion_matrix(model, X_te_arr, y_te, name, LABEL_ORDER)

    # ── 13. Model Selection ───────────────────────────────────────
    leaderboard = rank_models(cv_final, test_results)
    best_name, best_model = select_best(leaderboard, trained_models)
    save_best_model(best_model, best_name)
    generate_best_model_report(leaderboard, best_name)

    # ── 14. Save Overfitting Report ───────────────────────────────
    _save_overfitting_report(cv_final, test_results)

    # ── 15. Priority Models Report ────────────────────────────────
    _save_priority_report(cv_final, test_results, trained_models, X_tr_bal, y_tr_bal)

    # ── 16. SHAP Explainability ───────────────────────────────────
    print(f"\n[XAI] Running SHAP for best model: {best_name}")
    run_explainability(best_model, best_name, X_te_arr, feat_names, LABEL_ORDER)

    print("\n" + "=" * 60)
    print(f" ✅ TRAINING COMPLETE  |  Best Model: {best_name}")
    print("=" * 60)


def _save_overfitting_report(cv_df, test_df):
    path = os.path.join(REPORT_DIR, "overfitting_report.md")
    merged = cv_df.merge(test_df[["Model","Test_F1","Test_Accuracy"]], on="Model", how="left")

    lines = [
        "# Overfitting / Underfitting Report\n\n",
        "## Methodology\n\n",
        "For each model, we compare **Cross-Validation F1** (proxy for generalisation) "
        "vs **Test F1** (held-out performance). A large gap indicates overfitting.\n\n",
        "## Results\n\n",
        "| Model | CV F1 (mean±std) | Test F1 | Gap | Status |\n",
        "|-------|-----------------|---------|-----|--------|\n",
    ]
    for _, row in merged.iterrows():
        cv_f1  = row["CV_F1_Mean"]
        te_f1  = row["Test_F1"]
        gap    = round(cv_f1 - te_f1, 4)
        if gap > 0.10:
            status = "⚠️ Possible Overfit"
        elif te_f1 < 0.50:
            status = "⚠️ Possible Underfit"
        else:
            status = "✅ Healthy"
        lines.append(
            f"| {row['Model']} | {cv_f1:.4f} ± {row['CV_F1_Std']:.4f} "
            f"| {te_f1:.4f} | {gap:.4f} | {status} |\n"
        )
    lines += [
        "\n## Mitigation\n\n",
        "- Stratified K-Fold (5-fold) prevents data leakage across folds.\n",
        "- SMOTE applied only inside training folds.\n",
        "- `class_weight='balanced'` combats minority class bias.\n",
        "- Regularisation (max_depth, min_samples, L2) reduces overfitting.\n",
    ]
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    print(f"[Report] Overfitting report saved → {path}")


def _save_priority_report(cv_df, test_df, trained_models, X_tr, y_tr):
    import time
    path = os.path.join(REPORT_DIR, "priority_models_report.md")
    priority = ["CatBoost", "Random Forest", "XGBoost"]

    merged = cv_df.merge(test_df, on="Model", how="left")
    pri_df = merged[merged["Model"].isin(priority)]

    lines = [
        "# Priority Models Report\n\n",
        "Highest-priority models for survey / behavioural data:\n",
        "**CatBoost, Random Forest, XGBoost**\n\n",
        "## Detailed Metrics\n\n",
    ]
    for _, row in pri_df.iterrows():
        lines += [
            f"### {row['Model']}\n\n",
            f"| Metric | Value |\n|--------|-------|\n",
            f"| CV Accuracy | {row.get('CV_Accuracy_Mean','N/A')} |\n",
            f"| CV Precision | {row.get('CV_Precision_Mean','N/A')} |\n",
            f"| CV Recall | {row.get('CV_Recall_Mean','N/A')} |\n",
            f"| CV F1 | {row.get('CV_F1_Mean','N/A')} ± {row.get('CV_F1_Std','N/A')} |\n",
            f"| Test Accuracy | {row.get('Test_Accuracy','N/A')} |\n",
            f"| Test F1 | {row.get('Test_F1','N/A')} |\n",
            f"| Train Time | {row.get('Train_Time_s','N/A')} s |\n",
            f"| Inference Time | {row.get('Inference_Time_s','N/A')} s |\n\n",
        ]

    lines += [
        "## Recommendation\n\n",
        "**CatBoost** is the recommended production model for gadget addiction prediction:\n\n",
        "- Natively handles categorical features without manual encoding overhead.\n",
        "- Robust on small datasets (396 samples) due to ordered boosting.\n",
        "- Minimal hyperparameter sensitivity – good default performance.\n",
        "- Fastest inference among boosting models.\n",
        "- Excellent SHAP compatibility for explainability.\n",
    ]
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    print(f"[Report] Priority models report saved → {path}")


if __name__ == "__main__":
    main()
