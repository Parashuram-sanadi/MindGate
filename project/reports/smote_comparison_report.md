# Class Imbalance & SMOTE Report

**Imbalance ratio (majority/minority):** 3.081

## Class Counts Before SMOTE (training set)

| Class | Count |
|-------|-------|
| Moderate | 114 |
| Low | 108 |
| High | 37 |
| Severe | 57 |

## Class Counts After SMOTE

| Class | Count |
|-------|-------|
| Moderate | 114 |
| Low | 114 |
| High | 114 |
| Severe | 114 |

## Decision

SMOTE was applied.
SMOTE synthesises new minority-class samples using k-NN interpolation in feature space.
Applied only on training data to prevent data leakage into validation/test sets.
