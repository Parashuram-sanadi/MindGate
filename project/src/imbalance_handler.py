"""
src/imbalance_handler.py
Analyses class imbalance and applies SMOTE if needed.
Generates comparison report.
"""

import os, sys
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from collections import Counter
from imblearn.over_sampling import SMOTE

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import PLOT_DIR, REPORT_DIR, LABEL_ORDER, RANDOM_STATE


IMBALANCE_THRESHOLD = 1.5   # ratio of majority / minority class


def check_imbalance(y: np.ndarray) -> dict:
    counter   = Counter(y)
    counts    = list(counter.values())
    ratio     = max(counts) / min(counts)
    imbalanced = ratio > IMBALANCE_THRESHOLD
    return {
        "class_counts": dict(counter),
        "imbalance_ratio": round(ratio, 3),
        "imbalanced": imbalanced,
    }


def apply_smote(X_train: np.ndarray, y_train: np.ndarray):
    """Apply SMOTE to the training set. Returns resampled arrays."""
    sm = SMOTE(random_state=RANDOM_STATE, k_neighbors=3)
    X_res, y_res = sm.fit_resample(X_train, y_train)
    print(f"[SMOTE] Before: {Counter(y_train)}  →  After: {Counter(y_res)}")
    return X_res, y_res


def plot_imbalance_comparison(before: dict, after: dict, label_map: dict):
    """Save bar comparison of class counts before/after SMOTE."""
    os.makedirs(PLOT_DIR, exist_ok=True)
    labels = sorted(before.keys())
    before_vals = [before[k] for k in labels]
    after_vals  = [after[k]  for k in labels]
    names       = [label_map.get(k, str(k)) for k in labels]

    x   = np.arange(len(labels))
    w   = 0.35
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.bar(x - w/2, before_vals, w, label="Before SMOTE", color="#3498DB", edgecolor="white")
    ax.bar(x + w/2, after_vals,  w, label="After SMOTE",  color="#E74C3C", edgecolor="white")
    ax.set_xticks(x)
    ax.set_xticklabels(names)
    ax.set_title("Class Distribution Before vs After SMOTE", fontsize=14, fontweight="bold")
    ax.set_ylabel("Sample Count")
    ax.legend()
    for i, (b, a) in enumerate(zip(before_vals, after_vals)):
        ax.text(i - w/2, b + 1, str(b), ha="center", fontsize=9)
        ax.text(i + w/2, a + 1, str(a), ha="center", fontsize=9)
    plt.tight_layout()
    path = os.path.join(PLOT_DIR, "11_smote_comparison.png")
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"[SMOTE] Comparison plot saved → {path}")


def save_imbalance_report(info_before, info_after, label_map):
    os.makedirs(REPORT_DIR, exist_ok=True)
    path = os.path.join(REPORT_DIR, "smote_comparison_report.md")
    lines = [
        "# Class Imbalance & SMOTE Report\n\n",
        f"**Imbalance ratio (majority/minority):** {info_before['imbalance_ratio']}\n\n",
        "## Class Counts Before SMOTE (training set)\n\n",
        "| Class | Count |\n|-------|-------|\n",
    ]
    for k, v in info_before["class_counts"].items():
        lines.append(f"| {label_map.get(k, k)} | {v} |\n")

    lines += [
        "\n## Class Counts After SMOTE\n\n",
        "| Class | Count |\n|-------|-------|\n",
    ]
    for k, v in info_after.items():
        lines.append(f"| {label_map.get(k, k)} | {v} |\n")

    lines += [
        "\n## Decision\n\n",
        f"SMOTE was {'applied' if info_before['imbalanced'] else 'NOT applied (classes are balanced enough)'}.\n",
        "SMOTE synthesises new minority-class samples using k-NN interpolation in feature space.\n",
        "Applied only on training data to prevent data leakage into validation/test sets.\n",
    ]
    with open(path, "w") as f:
        f.writelines(lines)
    print(f"[SMOTE] Report saved → {path}")


def handle_imbalance(X_train, y_train, label_map=None):
    """
    Check imbalance. Apply SMOTE if needed. Return (X, y, was_resampled).
    label_map: {int_code: label_string}
    """
    label_map = label_map or {}
    info = check_imbalance(y_train)
    print(f"[Imbalance] Ratio = {info['imbalance_ratio']}  |  Imbalanced = {info['imbalanced']}")

    if info["imbalanced"]:
        X_res, y_res = apply_smote(X_train, y_train)
        after_counts = dict(Counter(y_res))
        plot_imbalance_comparison(info["class_counts"], after_counts, label_map)
        save_imbalance_report(info, after_counts, label_map)
        return X_res, y_res, True
    else:
        save_imbalance_report(info, info["class_counts"], label_map)
        return X_train, y_train, False
