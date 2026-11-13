"""
src/model_trainer.py
Trains all 11 classifier models, runs stratified 5-fold CV,
tunes priority models with Optuna, saves results.
"""

import os, sys, json, time, warnings
import numpy as np
import pandas as pd
import joblib
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
warnings.filterwarnings("ignore")

from sklearn.linear_model    import LogisticRegression
from sklearn.tree            import DecisionTreeClassifier
from sklearn.ensemble        import (RandomForestClassifier, ExtraTreesClassifier,
                                     GradientBoostingClassifier, AdaBoostClassifier)
from sklearn.svm             import SVC
from sklearn.neighbors       import KNeighborsClassifier
from sklearn.model_selection import StratifiedKFold, cross_validate
from sklearn.metrics         import (accuracy_score, precision_score, recall_score,
                                     f1_score, balanced_accuracy_score,
                                     roc_auc_score,
                                     confusion_matrix, classification_report)
import xgboost  as xgb
import lightgbm as lgb
from catboost import CatBoostClassifier

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import RANDOM_STATE, CV_FOLDS, OPTUNA_TRIALS, MODEL_DIR, REPORT_DIR, PLOT_DIR, LABEL_ORDER


# ─── Model Definitions ────────────────────────────────────────────────────────

def get_base_models(n_classes: int):
    return {
        "Logistic Regression": LogisticRegression(
            max_iter=2000, random_state=RANDOM_STATE, class_weight="balanced"),
        "Decision Tree": DecisionTreeClassifier(
            max_depth=6, random_state=RANDOM_STATE, class_weight="balanced"),
        "Random Forest": RandomForestClassifier(
            n_estimators=200, max_depth=8, random_state=RANDOM_STATE,
            class_weight="balanced", n_jobs=-1),
        "Extra Trees": ExtraTreesClassifier(
            n_estimators=200, random_state=RANDOM_STATE,
            class_weight="balanced", n_jobs=-1),
        "Gradient Boosting": GradientBoostingClassifier(
            n_estimators=150, random_state=RANDOM_STATE),
        "AdaBoost": AdaBoostClassifier(
            n_estimators=100, random_state=RANDOM_STATE),
        "XGBoost": xgb.XGBClassifier(
            n_estimators=200, max_depth=5, learning_rate=0.05,
            eval_metric="mlogloss",
            random_state=RANDOM_STATE, verbosity=0),
        "LightGBM": lgb.LGBMClassifier(
            n_estimators=200, max_depth=6, learning_rate=0.05,
            random_state=RANDOM_STATE, verbose=-1, class_weight="balanced"),
        "CatBoost": CatBoostClassifier(
            iterations=200, depth=6, learning_rate=0.05,
            random_seed=RANDOM_STATE, verbose=0, eval_metric="Accuracy"),
        "SVM": SVC(
            kernel="rbf", C=1.0, probability=True,
            class_weight="balanced", random_state=RANDOM_STATE),
        "KNN": KNeighborsClassifier(n_neighbors=7, weights="distance", n_jobs=-1),
    }


# ─── Cross Validation ─────────────────────────────────────────────────────────

def run_cross_validation(models: dict, X_train, y_train) -> pd.DataFrame:
    """5-fold stratified CV for all models. Returns DataFrame with stats."""
    skf     = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=RANDOM_STATE)
    scoring = {
        "accuracy": "accuracy",
        "precision_macro": "precision_macro",
        "recall_macro": "recall_macro",
        "f1_macro": "f1_macro",
        "roc_auc_ovr": "roc_auc_ovr"
    }
    records = []

    for name, model in models.items():
        print(f"[CV] {name} ...", end=" ", flush=True)
        t0  = time.time()
        cv  = cross_validate(model, X_train, y_train, cv=skf, scoring=scoring,
                             return_train_score=False, n_jobs=1)
        elapsed = round(time.time() - t0, 2)

        records.append({
            "Model":          name,
            "CV_Accuracy_Mean":    round(cv["test_accuracy"].mean(),         4),
            "CV_Accuracy_Std":     round(cv["test_accuracy"].std(),          4),
            "CV_Precision_Mean":   round(cv["test_precision_macro"].mean(),  4),
            "CV_Precision_Std":    round(cv["test_precision_macro"].std(),   4),
            "CV_Recall_Mean":      round(cv["test_recall_macro"].mean(),     4),
            "CV_Recall_Std":       round(cv["test_recall_macro"].std(),      4),
            "CV_F1_Mean":          round(cv["test_f1_macro"].mean(),         4),
            "CV_F1_Std":           round(cv["test_f1_macro"].std(),          4),
            "CV_ROC_AUC_Mean":     round(cv["test_roc_auc_ovr"].mean(),      4),
            "CV_ROC_AUC_Std":      round(cv["test_roc_auc_ovr"].std(),       4),
            "CV_Time_s":           elapsed,
        })
        print(f"F1={records[-1]['CV_F1_Mean']:.4f} ± {records[-1]['CV_F1_Std']:.4f}")

    df = pd.DataFrame(records)
    os.makedirs(REPORT_DIR, exist_ok=True)
    df.to_csv(os.path.join(REPORT_DIR, "cross_validation_results.csv"), index=False)
    print(f"[CV] Results saved.")
    return df


# ─── Optuna Hyperparameter Tuning ─────────────────────────────────────────────

def tune_with_optuna(X_train, y_train) -> dict:
    """Tune RF, XGBoost, LightGBM, CatBoost via Optuna."""
    import optuna
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    skf = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=RANDOM_STATE)
    best_params = {}

    # ── Random Forest ──────────────────────────────────────────────────────────
    def rf_objective(trial):
        params = dict(
            n_estimators    = trial.suggest_int("n_estimators", 100, 300),
            max_depth       = trial.suggest_int("max_depth", 3, 12),
            min_samples_split = trial.suggest_int("min_samples_split", 2, 10),
            min_samples_leaf  = trial.suggest_int("min_samples_leaf", 1, 6),
            max_features    = trial.suggest_categorical("max_features", ["sqrt", "log2", None]),
        )
        model = RandomForestClassifier(**params, random_state=RANDOM_STATE,
                                       class_weight="balanced", n_jobs=-1)
        scores = cross_validate(model, X_train, y_train, cv=skf,
                                scoring="f1_macro", n_jobs=1)
        return scores["test_score"].mean()

    print("[Optuna] Tuning Random Forest ...")
    study_rf = optuna.create_study(direction="maximize")
    study_rf.optimize(rf_objective, n_trials=OPTUNA_TRIALS, show_progress_bar=False)
    best_params["Random Forest"] = study_rf.best_params
    print(f"  Best RF F1 = {study_rf.best_value:.4f}")

    # ── XGBoost ───────────────────────────────────────────────────────────────
    def xgb_objective(trial):
        params = dict(
            n_estimators    = trial.suggest_int("n_estimators", 100, 300),
            max_depth       = trial.suggest_int("max_depth", 3, 9),
            learning_rate   = trial.suggest_float("learning_rate", 0.01, 0.3, log=True),
            subsample       = trial.suggest_float("subsample", 0.5, 1.0),
            colsample_bytree= trial.suggest_float("colsample_bytree", 0.5, 1.0),
            gamma           = trial.suggest_float("gamma", 0, 5),
            reg_alpha       = trial.suggest_float("reg_alpha", 0, 1),
        )
        model = xgb.XGBClassifier(**params,
                                  eval_metric="mlogloss", random_state=RANDOM_STATE, verbosity=0)
        scores = cross_validate(model, X_train, y_train, cv=skf,
                                scoring="f1_macro", n_jobs=1)
        return scores["test_score"].mean()

    print("[Optuna] Tuning XGBoost ...")
    study_xgb = optuna.create_study(direction="maximize")
    study_xgb.optimize(xgb_objective, n_trials=OPTUNA_TRIALS, show_progress_bar=False)
    best_params["XGBoost"] = study_xgb.best_params
    print(f"  Best XGB F1 = {study_xgb.best_value:.4f}")

    # ── LightGBM ──────────────────────────────────────────────────────────────
    def lgb_objective(trial):
        params = dict(
            n_estimators    = trial.suggest_int("n_estimators", 100, 300),
            max_depth       = trial.suggest_int("max_depth", 3, 9),
            learning_rate   = trial.suggest_float("learning_rate", 0.01, 0.3, log=True),
            num_leaves      = trial.suggest_int("num_leaves", 15, 127),
            subsample       = trial.suggest_float("subsample", 0.5, 1.0),
            colsample_bytree= trial.suggest_float("colsample_bytree", 0.5, 1.0),
            reg_alpha       = trial.suggest_float("reg_alpha", 0, 1),
            reg_lambda      = trial.suggest_float("reg_lambda", 0, 1),
        )
        model = lgb.LGBMClassifier(**params, random_state=RANDOM_STATE, verbose=-1,
                                    class_weight="balanced")
        scores = cross_validate(model, X_train, y_train, cv=skf,
                                scoring="f1_macro", n_jobs=1)
        return scores["test_score"].mean()

    print("[Optuna] Tuning LightGBM ...")
    study_lgb = optuna.create_study(direction="maximize")
    study_lgb.optimize(lgb_objective, n_trials=OPTUNA_TRIALS, show_progress_bar=False)
    best_params["LightGBM"] = study_lgb.best_params
    print(f"  Best LGB F1 = {study_lgb.best_value:.4f}")

    # ── CatBoost ──────────────────────────────────────────────────────────────
    def cat_objective(trial):
        params = dict(
            iterations      = trial.suggest_int("iterations", 100, 250),
            depth           = trial.suggest_int("depth", 3, 7),
            learning_rate   = trial.suggest_float("learning_rate", 0.01, 0.3, log=True),
            l2_leaf_reg     = trial.suggest_float("l2_leaf_reg", 1, 10),
            subsample       = trial.suggest_float("subsample", 0.5, 1.0),
        )
        model = CatBoostClassifier(**params, bootstrap_type="Bernoulli",
                                   random_seed=RANDOM_STATE, verbose=0)
        scores = cross_validate(model, X_train, y_train, cv=skf,
                                scoring="f1_macro", n_jobs=1)
        return scores["test_score"].mean()

    print("[Optuna] Tuning CatBoost ...")
    study_cat = optuna.create_study(direction="maximize")
    study_cat.optimize(cat_objective, n_trials=OPTUNA_TRIALS, show_progress_bar=False)
    best_params["CatBoost"] = study_cat.best_params
    print(f"  Best CAT F1 = {study_cat.best_value:.4f}")

    # Save
    os.makedirs(MODEL_DIR, exist_ok=True)
    with open(os.path.join(MODEL_DIR, "best_parameters.json"), "w") as f:
        json.dump(best_params, f, indent=2)
    print(f"[Optuna] Best parameters saved.")
    return best_params


# ─── Build Tuned Models ───────────────────────────────────────────────────────

def build_tuned_models(best_params: dict):
    """Return model dict replacing priority models with tuned versions."""
    tuned = {}

    if "Random Forest" in best_params:
        p = best_params["Random Forest"]
        tuned["Random Forest"] = RandomForestClassifier(
            **p, random_state=RANDOM_STATE, class_weight="balanced", n_jobs=-1)

    if "XGBoost" in best_params:
        p = best_params["XGBoost"]
        tuned["XGBoost"] = xgb.XGBClassifier(
            **p, eval_metric="mlogloss",
            random_state=RANDOM_STATE, verbosity=0)

    if "LightGBM" in best_params:
        p = best_params["LightGBM"]
        tuned["LightGBM"] = lgb.LGBMClassifier(
            **p, random_state=RANDOM_STATE, verbose=-1, class_weight="balanced")

    if "CatBoost" in best_params:
        p = best_params["CatBoost"]
        tuned["CatBoost"] = CatBoostClassifier(
            **p, bootstrap_type="Bernoulli", random_seed=RANDOM_STATE, verbose=0)

    return tuned


# ─── Fit & Evaluate ───────────────────────────────────────────────────────────

def fit_and_evaluate(models: dict, X_train, y_train, X_test, y_test, label_names) -> pd.DataFrame:
    """Fit each model and compute test-set metrics. Returns DataFrame."""
    records  = []
    trained  = {}

    for name, model in models.items():
        print(f"[Train] Fitting {name} ...", end=" ", flush=True)
        t0 = time.time()
        model.fit(X_train, y_train)
        train_time = round(time.time() - t0, 3)

        t1 = time.time()
        y_pred = model.predict(X_test)
        y_prob = model.predict_proba(X_test) if hasattr(model, "predict_proba") else None
        inf_time = round(time.time() - t1, 6)

        roc_auc = "N/A"
        if y_prob is not None:
            try:
                roc_auc = round(roc_auc_score(y_test, y_prob, multi_class="ovr", average="macro"), 4)
            except:
                pass

        records.append({
            "Model":            name,
            "Test_Accuracy":    round(accuracy_score(y_test, y_pred),                             4),
            "Test_Precision":   round(precision_score(y_test, y_pred, average="macro", zero_division=0), 4),
            "Test_Recall":      round(recall_score(y_test, y_pred, average="macro", zero_division=0),    4),
            "Test_F1":          round(f1_score(y_test, y_pred, average="macro", zero_division=0),        4),
            "Test_ROC_AUC":     roc_auc,
            "Balanced_Acc":     round(balanced_accuracy_score(y_test, y_pred),                    4),
            "Train_Time_s":     train_time,
            "Inference_Time_s": inf_time,
        })
        trained[name] = model
        print(f"Acc={records[-1]['Test_Accuracy']:.4f}  F1={records[-1]['Test_F1']:.4f}")

    df = pd.DataFrame(records)
    os.makedirs(REPORT_DIR, exist_ok=True)
    df.to_csv(os.path.join(REPORT_DIR, "model_comparison.csv"), index=False)
    return df, trained


# ─── Learning Curves ──────────────────────────────────────────────────────────

def plot_learning_curve(model, model_name: str, X, y):
    """Generate and save a learning curve to detect over/underfitting."""
    from sklearn.model_selection import learning_curve
    sizes, tr_scores, val_scores = learning_curve(
        model, X, y,
        cv=StratifiedKFold(n_splits=5, shuffle=True, random_state=RANDOM_STATE),
        scoring="f1_macro",
        train_sizes=np.linspace(0.2, 1.0, 8),
        n_jobs=1
    )
    tr_mean  = tr_scores.mean(axis=1)
    val_mean = val_scores.mean(axis=1)
    tr_std   = tr_scores.std(axis=1)
    val_std  = val_scores.std(axis=1)

    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(sizes, tr_mean,  "o-", color="#3498DB", label="Train F1")
    ax.plot(sizes, val_mean, "s-", color="#E74C3C", label="Validation F1")
    ax.fill_between(sizes, tr_mean - tr_std,  tr_mean + tr_std,  alpha=0.15, color="#3498DB")
    ax.fill_between(sizes, val_mean - val_std, val_mean + val_std, alpha=0.15, color="#E74C3C")
    ax.set_title(f"Learning Curve – {model_name}", fontsize=13, fontweight="bold")
    ax.set_xlabel("Training Set Size")
    ax.set_ylabel("Macro F1 Score")
    ax.legend()
    plt.tight_layout()

    safe_name = model_name.replace(" ", "_")
    path      = os.path.join(PLOT_DIR, f"lc_{safe_name}.png")
    os.makedirs(PLOT_DIR, exist_ok=True)
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"[LearningCurve] Saved → {path}")


# ─── Confusion Matrix ─────────────────────────────────────────────────────────

def plot_confusion_matrix(model, X_test, y_test, model_name: str, label_names):
    import seaborn as sns
    y_pred = model.predict(X_test)
    cm     = confusion_matrix(y_test, y_pred)
    fig, ax = plt.subplots(figsize=(7, 6))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=label_names, yticklabels=label_names, ax=ax)
    ax.set_title(f"Confusion Matrix – {model_name}", fontsize=13, fontweight="bold")
    ax.set_ylabel("Actual")
    ax.set_xlabel("Predicted")
    plt.tight_layout()
    safe_name = model_name.replace(" ", "_")
    path = os.path.join(PLOT_DIR, f"cm_{safe_name}.png")
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"[CM] Saved → {path}")
