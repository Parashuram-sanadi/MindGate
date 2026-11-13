# Feature Engineering Report

## Dataset Dimensions
- Raw features (before encoding): **44**
- Encoded feature dimensions: **65**

## Missing Values

| Column | Missing Count |
|--------|---------------|
| `Q28_PhysicalSymptoms` | 69 |

Missing values imputed using **median** (numeric) and **most_frequent** (categorical).

## Duplicate Records
Duplicate rows detected: **3**
(Duplicates kept because dataset is small; they may represent genuine repeated survey responses.)

## Encoding Strategy

| Type | Strategy |
|------|----------|
| Ordered categoricals (ScreenTime, CGPA, etc.) | Ordinal integer mapping |
| Frequency responses (Never→Always) | Integer 0–5 |
| Yes/No agreement responses | Integer 0–2 |
| Multi-value text (GadgetUses, Symptoms) | Item count |
| Nominal (Gender, Department) | One-Hot Encoding |
| Numeric | StandardScaler (mean=0, std=1) |

## Feature Selection

Features with near-zero variance are removed by the tree models automatically. SHAP importance analysis performed post-training.

## Outlier Treatment

Outliers are retained. StandardScaler clips extreme values implicitly via z-score normalization. Tree models are inherently robust to outliers.
