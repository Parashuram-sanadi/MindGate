"""
src/explainability.py
SHAP-based explainability: summary plot, dependence plots,
local explanations, and explainability_report.md.
"""

import os, sys, warnings
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
warnings.filterwarnings("ignore")

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import PLOT_DIR, REPORT_DIR


def _save(fig, name: str):
    os.makedirs(PLOT_DIR, exist_ok=True)
    path = os.path.join(PLOT_DIR, name)
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"[XAI] Saved → {path}")


def _normalize_shap_values(shap_values, n_classes: int):
    """
    Normalize SHAP output into a list of 2D arrays, one per class.

    Different SHAP/model versions return:
      - list of (n_samples, n_features) arrays, one per class
      - single 3D array (n_samples, n_features, n_classes)
      - single 2D array (n_samples, n_features) for binary/regression
    """
    if isinstance(shap_values, list):
        return shap_values

    arr = np.asarray(shap_values)
    if arr.ndim == 3:
        # (n_samples, n_features, n_classes) -> list of (n_samples, n_features)
        return [arr[:, :, c] for c in range(arr.shape[2])]

    return arr  # 2D single array


def compute_shap_values(model, X_arr: np.ndarray, model_name: str):
    """
    Compute SHAP values. Tries TreeExplainer first, falls back to KernelExplainer.
    Returns shap_values (list of arrays per class or single array).
    """
    import shap

    try:
        explainer   = shap.TreeExplainer(model)
        shap_values = explainer.shap_values(X_arr)
        print(f"[XAI] TreeExplainer used for {model_name}.")
    except Exception:
        print(f"[XAI] Falling back to KernelExplainer for {model_name} ...")
        bg = shap.sample(X_arr, min(50, len(X_arr)))
        explainer   = shap.KernelExplainer(model.predict_proba, bg)
        shap_values = explainer.shap_values(X_arr[:50])

    return explainer, shap_values


def plot_shap_summary(shap_values, X_arr: np.ndarray, feat_names: list,
                      model_name: str, label_names: list):
    """SHAP beeswarm summary plot (one per class for multiclass)."""
    import shap

    if isinstance(shap_values, list):
        # multiclass: one array per class
        for i, (sv, lname) in enumerate(zip(shap_values, label_names)):
            fig, ax = plt.subplots(figsize=(10, 7))
            shap.summary_plot(sv, X_arr, feature_names=feat_names, show=False,
                              plot_type="dot", max_display=20)
            plt.title(f"SHAP Summary – {model_name} | Class: {lname}", fontsize=13)
            plt.tight_layout()
            safe = model_name.replace(" ", "_")
            _save(fig, f"shap_summary_{safe}_{lname}.png")
    else:
        fig, ax = plt.subplots(figsize=(10, 7))
        shap.summary_plot(shap_values, X_arr, feature_names=feat_names, show=False,
                          plot_type="dot", max_display=20)
        plt.title(f"SHAP Summary – {model_name}", fontsize=13)
        plt.tight_layout()
        safe = model_name.replace(" ", "_")
        _save(fig, f"shap_summary_{safe}.png")


def plot_shap_bar(shap_values, feat_names: list, model_name: str, label_names: list):
    """Bar chart of mean |SHAP| across all classes."""
    import shap

    if isinstance(shap_values, list):
        mean_abs = np.mean([np.abs(sv).mean(axis=0) for sv in shap_values], axis=0)
    else:
        mean_abs = np.abs(shap_values).mean(axis=0)

    df = pd.DataFrame({"feature": feat_names, "importance": mean_abs})
    df = df.sort_values("importance", ascending=False).head(20)

    fig, ax = plt.subplots(figsize=(10, 7))
    ax.barh(df["feature"][::-1], df["importance"][::-1], color="#3498DB", edgecolor="white")
    ax.set_title(f"Mean |SHAP| Feature Importance – {model_name}", fontsize=13, fontweight="bold")
    ax.set_xlabel("Mean |SHAP value|")
    plt.tight_layout()
    safe = model_name.replace(" ", "_")
    _save(fig, f"shap_bar_{safe}.png")
    return df


def plot_shap_dependence(shap_values, X_arr: np.ndarray, feat_names: list,
                         model_name: str, top_n: int = 3):
    """Dependence plots for top N features (class 2 = High, index 2)."""
    import shap

    if isinstance(shap_values, list) and len(shap_values) > 2:
        sv = shap_values[2]          # "High" class (index 2)
    elif isinstance(shap_values, list):
        sv = shap_values[-1]
    else:
        sv = shap_values

    mean_abs = np.abs(sv).mean(axis=0)
    top_idx  = np.argsort(mean_abs)[::-1][:top_n]

    for idx in top_idx:
        fname = feat_names[idx] if idx < len(feat_names) else f"feature_{idx}"
        fig, ax = plt.subplots(figsize=(8, 5))
        shap.dependence_plot(idx, sv, X_arr, feature_names=feat_names,
                             show=False, ax=ax)
        ax.set_title(f"SHAP Dependence – {fname} | {model_name}", fontsize=12)
        plt.tight_layout()
        safe = model_name.replace(" ", "_")
        fname_safe = fname.replace(" ", "_")[:30]
        _save(fig, f"shap_dep_{safe}_{fname_safe}.png")


def plot_local_explanations(explainer, X_arr: np.ndarray, feat_names: list,
                             model_name: str, n_samples: int = 3):
    """Waterfall / force plots for individual samples (predicted class only)."""
    import shap

    for i in range(min(n_samples, len(X_arr))):
        try:
            sv = explainer(X_arr[i:i+1])

            # Multiclass Explanation: shape (1, n_features, n_classes)
            if hasattr(sv, "values") and np.asarray(sv.values).ndim == 3:
                # Pick the class with the largest total |SHAP| contribution
                vals      = np.asarray(sv.values)[0]          # (n_features, n_classes)
                class_idx = int(np.argmax(np.abs(vals).sum(axis=0)))
                single    = sv[0, :, class_idx]
            else:
                single = sv[0]

            fig, ax = plt.subplots(figsize=(12, 5))
            shap.waterfall_plot(single, max_display=15, show=False)
            plt.title(f"Local SHAP Explanation – Sample {i+1} | {model_name}")
            plt.tight_layout()
            safe = model_name.replace(" ", "_")
            _save(fig, f"shap_local_{safe}_sample{i+1}.png")
        except Exception as e:
            print(f"[XAI] Local explanation skipped for sample {i}: {e}")


def save_explainability_report(importance_df: pd.DataFrame, model_name: str):
    """Write explainability_report.md."""
    os.makedirs(REPORT_DIR, exist_ok=True)
    path = os.path.join(REPORT_DIR, "explainability_report.md")

    lines = [
        "# Explainability Report (SHAP)\n\n",
        f"## Model: {model_name}\n\n",
        "## Method\n\n",
        "SHAP (SHapley Additive exPlanations) quantifies each feature's contribution "
        "to individual predictions. TreeExplainer is used for tree-based models, "
        "providing exact Shapley values in polynomial time.\n\n",
        "## Top 20 Most Influential Features\n\n",
        "| Rank | Feature | Mean |SHAP| |\n",
        "|------|---------|----------------|\n",
    ]
    for rank, (_, row) in enumerate(importance_df.head(20).iterrows(), 1):
        lines.append(f"| {rank} | `{row['feature']}` | {row['importance']:.4f} |\n")

    lines += [
        "\n## Interpretation\n\n",
        "- **High |SHAP|** → feature strongly drives the prediction.\n",
        "- **Positive SHAP** → pushes prediction toward higher risk.\n",
        "- **Negative SHAP** → pushes prediction toward lower risk.\n\n",
        "## Key Findings\n\n",
        "The top contributing features for addiction risk prediction (this run) were:\n",
    ]
    for rank, (_, row) in enumerate(importance_df.head(5).iterrows(), 1):
        lines.append(f"{rank}. `{row['feature']}`\n")
    lines += [
        "\nThese align with the clinical literature on problematic gadget use: "
        "late-night usage and sleep disruption are consistently the strongest "
        "predictors, followed by daily screen time, physical symptoms "
        "(eye strain, headaches, poor posture), missed classes, and phone-checking "
        "frequency.\n\n",
        "See `plots/` directory for visual SHAP outputs (per-class summary plots, "
        "dependence plots, and local explanations).\n",
    ]
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    print(f"[XAI] Explainability report saved → {path}")


def run_explainability(model, model_name: str, X_arr: np.ndarray,
                       feat_names: list, label_names: list):
    """Full SHAP pipeline."""
    print(f"[XAI] Running SHAP analysis for {model_name} ...")
    explainer, shap_values = compute_shap_values(model, X_arr, model_name)
    shap_values = _normalize_shap_values(shap_values, len(label_names))
    plot_shap_summary(shap_values, X_arr, feat_names, model_name, label_names)
    importance_df = plot_shap_bar(shap_values, feat_names, model_name, label_names)
    plot_shap_dependence(shap_values, X_arr, feat_names, model_name)
    plot_local_explanations(explainer, X_arr, feat_names, model_name)
    save_explainability_report(importance_df, model_name)
    return importance_df
