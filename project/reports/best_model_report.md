# Best Model Report

## Selected Model: **CatBoost**

### Selection Criteria
Models were ranked by:
1. Cross-Validation Macro F1 (primary)
2. Cross-Validation Accuracy
3. Test Macro F1
4. Test Accuracy

### Performance Summary

| Metric | Value |
|--------|-------|
| CV F1 Mean | 0.9758 |
| CV F1 Std  | 0.0162 |
| CV Accuracy| 0.9759 |
| Test F1    | 0.8969 |
| Test Acc   | 0.925 |
| Balanced Acc| 0.8828 |
| Train Time | 0.376 s |

### Why This Model?

**CatBoost** was selected because it achieved the highest cross-validation F1 score among all trained models, indicating it generalises best to unseen data on this survey-based dataset.

For gadget addiction prediction specifically:

- The dataset contains predominantly **ordinal and categorical survey features**.
- Gradient-boosted tree ensembles handle heterogeneous feature types robustly.
- **CatBoost** and **Random Forest** are especially well-suited because:
  - They require minimal preprocessing of categorical variables.
  - They are robust to small datasets (~396 samples).
  - They naturally handle class imbalance via `class_weight`.
  - Their predictions are interpretable via SHAP.

### Model Leaderboard (Top 5)

|   Rank | Model         |   CV_F1_Mean |   CV_Accuracy_Mean |   Test_F1 |   Test_Accuracy |
|-------:|:--------------|-------------:|-------------------:|----------:|----------------:|
|      1 | CatBoost      |       0.9758 |             0.9759 |    0.8969 |          0.925  |
|      2 | Extra Trees   |       0.965  |             0.965  |    0.8917 |          0.925  |
|      3 | LightGBM      |       0.9649 |             0.9649 |    0.8465 |          0.9    |
|      4 | Random Forest |       0.9648 |             0.965  |    0.873  |          0.9125 |
|      5 | SVM           |       0.9626 |             0.9627 |    0.9278 |          0.95   |
