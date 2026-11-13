"""
src/eda.py
Generates comprehensive EDA plots and statistical summary.
"""

import os, sys
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from configs.config import TARGET_COL, LEAKAGE_COLS, META_COLS, PLOT_DIR, REPORT_DIR, LABEL_ORDER

sns.set_theme(style="whitegrid", palette="Set2")
PALETTE = {"Low": "#2ECC71", "Moderate": "#F39C12", "High": "#E74C3C", "Severe": "#8E44AD"}


def _save(fig, name: str):
    os.makedirs(PLOT_DIR, exist_ok=True)
    path = os.path.join(PLOT_DIR, name)
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"[EDA] Saved plot → {path}")


def plot_class_distribution(df: pd.DataFrame):
    """Bar + pie chart of target label distribution."""
    counts = df[TARGET_COL].value_counts().reindex(LABEL_ORDER)

    fig, axes = plt.subplots(1, 2, figsize=(12, 5))
    colors = [PALETTE[l] for l in LABEL_ORDER]

    axes[0].bar(LABEL_ORDER, counts.values, color=colors, edgecolor="white", linewidth=1.2)
    for i, v in enumerate(counts.values):
        axes[0].text(i, v + 1.5, str(v), ha="center", fontsize=11, fontweight="bold")
    axes[0].set_title("Class Distribution (Count)", fontsize=14, fontweight="bold")
    axes[0].set_ylabel("Count")
    axes[0].set_xlabel("Risk Level")

    axes[1].pie(
        counts.values,
        labels=LABEL_ORDER,
        colors=colors,
        autopct="%1.1f%%",
        startangle=140,
        wedgeprops={"edgecolor": "white"},
    )
    axes[1].set_title("Class Distribution (Proportion)", fontsize=14, fontweight="bold")

    fig.suptitle("Electronic Gadget Addiction – Label Distribution", fontsize=16, fontweight="bold")
    plt.tight_layout()
    _save(fig, "01_class_distribution.png")


def plot_missing_values(df: pd.DataFrame):
    """Heatmap of missing values."""
    exclude = META_COLS + LEAKAGE_COLS + [TARGET_COL]
    cols    = [c for c in df.columns if c not in exclude]
    missing = df[cols].isnull()

    if missing.values.sum() == 0:
        print("[EDA] No missing values – skipping missing heatmap.")
        return

    fig, ax = plt.subplots(figsize=(14, 6))
    sns.heatmap(missing.T, cbar=False, yticklabels=True, xticklabels=False,
                cmap="YlOrRd", ax=ax)
    ax.set_title("Missing Value Heatmap (yellow = missing)", fontsize=14, fontweight="bold")
    ax.set_xlabel("Records")
    ax.set_ylabel("Features")
    plt.tight_layout()
    _save(fig, "02_missing_values_heatmap.png")


def plot_screen_time_vs_risk(df: pd.DataFrame):
    """Grouped bar of screen time vs risk level."""
    if "Q7_ScreenTime" not in df.columns:
        return
    ct = pd.crosstab(df["Q7_ScreenTime"], df[TARGET_COL], normalize="index") * 100
    ct = ct.reindex(columns=LABEL_ORDER, fill_value=0)

    fig, ax = plt.subplots(figsize=(11, 5))
    ct.plot(kind="bar", ax=ax, color=[PALETTE[l] for l in LABEL_ORDER],
            edgecolor="white", linewidth=0.8)
    ax.set_title("Screen Time vs Risk Level (%)", fontsize=14, fontweight="bold")
    ax.set_xlabel("Daily Screen Time")
    ax.set_ylabel("% of Students")
    ax.legend(title="Risk Level", bbox_to_anchor=(1.01, 1))
    plt.xticks(rotation=30)
    plt.tight_layout()
    _save(fig, "03_screen_time_vs_risk.png")


def plot_sleep_distribution(df: pd.DataFrame):
    """Box plot of sleep hours per risk class."""
    if "sleep_hours" not in df.columns:
        return
    fig, ax = plt.subplots(figsize=(9, 5))
    order = [l for l in LABEL_ORDER if l in df[TARGET_COL].unique()]
    colors = [PALETTE[l] for l in order]
    bplot  = ax.boxplot(
        [df[df[TARGET_COL] == l]["sleep_hours"].dropna().values for l in order],
        tick_labels=order, patch_artist=True, notch=True
    )
    for patch, color in zip(bplot["boxes"], colors):
        patch.set_facecolor(color)
    ax.set_title("Sleep Hours by Addiction Risk Level", fontsize=14, fontweight="bold")
    ax.set_ylabel("Sleep Hours")
    ax.set_xlabel("Risk Level")
    plt.tight_layout()
    _save(fig, "04_sleep_hours_by_risk.png")


def plot_stress_vs_risk(df: pd.DataFrame):
    """Stacked bar of stress level vs risk."""
    if "Q30_StressLevel" not in df.columns:
        return
    ct = pd.crosstab(df["Q30_StressLevel"], df[TARGET_COL], normalize="index") * 100
    ct = ct.reindex(columns=LABEL_ORDER, fill_value=0)

    fig, ax = plt.subplots(figsize=(9, 5))
    ct.plot(kind="bar", stacked=True, ax=ax,
            color=[PALETTE[l] for l in LABEL_ORDER], edgecolor="white")
    ax.set_title("Stress Level vs Addiction Risk (%)", fontsize=14, fontweight="bold")
    ax.set_xlabel("Stress Level (1=Low, 5=High)")
    ax.set_ylabel("% Students")
    ax.legend(title="Risk Level", bbox_to_anchor=(1.01, 1))
    plt.xticks(rotation=0)
    plt.tight_layout()
    _save(fig, "05_stress_vs_risk.png")


def plot_correlation_heatmap(df: pd.DataFrame):
    """Heatmap of numeric feature correlations."""
    exclude = META_COLS + LEAKAGE_COLS + [TARGET_COL]
    num_df  = df.drop(columns=[c for c in exclude if c in df.columns])
    num_df  = num_df.select_dtypes(include=np.number)

    if num_df.shape[1] < 3:
        return

    corr = num_df.corr()
    mask = np.triu(np.ones_like(corr, dtype=bool))

    fig, ax = plt.subplots(figsize=(14, 10))
    sns.heatmap(
        corr, mask=mask, annot=True, fmt=".2f", cmap="coolwarm",
        center=0, square=True, linewidths=0.5, ax=ax,
        annot_kws={"size": 7},
    )
    ax.set_title("Feature Correlation Heatmap (numeric features)", fontsize=14, fontweight="bold")
    plt.tight_layout()
    _save(fig, "06_correlation_heatmap.png")


def plot_gender_distribution(df: pd.DataFrame):
    """Grouped bar of gender vs risk level."""
    if "Q2_Gender" not in df.columns:
        return
    ct = pd.crosstab(df["Q2_Gender"], df[TARGET_COL])
    ct = ct.reindex(columns=LABEL_ORDER, fill_value=0)

    fig, ax = plt.subplots(figsize=(8, 5))
    ct.plot(kind="bar", ax=ax, color=[PALETTE[l] for l in LABEL_ORDER],
            edgecolor="white")
    ax.set_title("Gender vs Addiction Risk Level", fontsize=14, fontweight="bold")
    ax.set_xlabel("Gender")
    ax.set_ylabel("Count")
    ax.legend(title="Risk Level")
    plt.xticks(rotation=0)
    plt.tight_layout()
    _save(fig, "07_gender_vs_risk.png")


def plot_cgpa_vs_risk(df: pd.DataFrame):
    """Box plot of CGPA by risk."""
    if "Q16_CGPA" not in df.columns:
        return
    cgpa_map = {"<5": 4.5, "5-6": 5.5, "6-7": 6.5, "7-8": 7.5, "8-9": 8.5, "9-10": 9.5}
    tmp = df.copy()
    tmp["cgpa_num"] = tmp["Q16_CGPA"].map(cgpa_map)

    fig, ax = plt.subplots(figsize=(9, 5))
    order  = [l for l in LABEL_ORDER if l in tmp[TARGET_COL].unique()]
    colors = [PALETTE[l] for l in order]
    bplot  = ax.boxplot(
        [tmp[tmp[TARGET_COL] == l]["cgpa_num"].dropna().values for l in order],
        tick_labels=order, patch_artist=True
    )
    for patch, color in zip(bplot["boxes"], colors):
        patch.set_facecolor(color)
    ax.set_title("CGPA by Addiction Risk Level", fontsize=14, fontweight="bold")
    ax.set_ylabel("CGPA (midpoint)")
    ax.set_xlabel("Risk Level")
    plt.tight_layout()
    _save(fig, "08_cgpa_vs_risk.png")


def plot_year_vs_risk(df: pd.DataFrame):
    """Grouped bar of academic year vs risk."""
    if "Q3_YearOfStudy" not in df.columns:
        return
    ct = pd.crosstab(df["Q3_YearOfStudy"], df[TARGET_COL])
    order_rows = ["1st Year", "2nd Year", "3rd Year", "4th Year"]
    ct = ct.reindex(index=order_rows, columns=LABEL_ORDER, fill_value=0)

    fig, ax = plt.subplots(figsize=(10, 5))
    ct.plot(kind="bar", ax=ax, color=[PALETTE[l] for l in LABEL_ORDER], edgecolor="white")
    ax.set_title("Year of Study vs Addiction Risk", fontsize=14, fontweight="bold")
    ax.set_xlabel("Year of Study")
    ax.set_ylabel("Count")
    ax.legend(title="Risk Level", bbox_to_anchor=(1.01, 1))
    plt.xticks(rotation=15)
    plt.tight_layout()
    _save(fig, "09_year_vs_risk.png")


def plot_phone_checks(df: pd.DataFrame):
    """Phone checks per hour vs risk level."""
    if "Q11_PhoneChecks_PerHr" not in df.columns:
        return
    ct = pd.crosstab(df["Q11_PhoneChecks_PerHr"], df[TARGET_COL], normalize="index") * 100
    ct = ct.reindex(columns=LABEL_ORDER, fill_value=0)

    fig, ax = plt.subplots(figsize=(9, 5))
    ct.plot(kind="bar", stacked=True, ax=ax,
            color=[PALETTE[l] for l in LABEL_ORDER], edgecolor="white")
    ax.set_title("Phone Checks per Hour vs Risk Level", fontsize=14, fontweight="bold")
    ax.set_xlabel("Phone Checks / Hour")
    ax.set_ylabel("% Students")
    ax.legend(title="Risk Level", bbox_to_anchor=(1.01, 1))
    plt.xticks(rotation=0)
    plt.tight_layout()
    _save(fig, "10_phone_checks_vs_risk.png")


def save_statistical_summary(df: pd.DataFrame):
    """Save a statistical summary CSV."""
    os.makedirs(REPORT_DIR, exist_ok=True)
    exclude = META_COLS + LEAKAGE_COLS
    num_df  = df.drop(columns=[c for c in exclude if c in df.columns])
    num_df  = num_df.select_dtypes(include=np.number)
    desc    = num_df.describe(percentiles=[0.05, 0.25, 0.50, 0.75, 0.95]).T
    path    = os.path.join(REPORT_DIR, "statistical_summary.csv")
    desc.to_csv(path)
    print(f"[EDA] Statistical summary → {path}")


def run_eda(df: pd.DataFrame):
    """Run all EDA routines."""
    print("[EDA] Running comprehensive exploratory data analysis ...")
    plot_class_distribution(df)
    plot_missing_values(df)
    plot_screen_time_vs_risk(df)
    plot_sleep_distribution(df)
    plot_stress_vs_risk(df)
    plot_correlation_heatmap(df)
    plot_gender_distribution(df)
    plot_cgpa_vs_risk(df)
    plot_year_vs_risk(df)
    plot_phone_checks(df)
    save_statistical_summary(df)
    print("[EDA] All EDA plots generated.")
