# Overfitting / Underfitting Report

## Methodology

For each model, we compare **Cross-Validation F1** (proxy for generalisation) vs **Test F1** (held-out performance). A large gap indicates overfitting.

## Results

| Model | CV F1 (mean±std) | Test F1 | Gap | Status |
|-------|-----------------|---------|-----|--------|
| Logistic Regression | 0.9537 ± 0.0105 | 0.8776 | 0.0761 | ✅ Healthy |
| Decision Tree | 0.8888 ± 0.0159 | 0.8394 | 0.0494 | ✅ Healthy |
| Random Forest | 0.9648 ± 0.0128 | 0.8730 | 0.0918 | ✅ Healthy |
| Extra Trees | 0.9650 ± 0.0125 | 0.8917 | 0.0733 | ✅ Healthy |
| Gradient Boosting | 0.9625 ± 0.0148 | 0.8465 | 0.1160 | ⚠️ Possible Overfit |
| AdaBoost | 0.5488 ± 0.0768 | 0.5291 | 0.0197 | ✅ Healthy |
| XGBoost | 0.9582 ± 0.0081 | 0.8730 | 0.0852 | ✅ Healthy |
| LightGBM | 0.9649 ± 0.0127 | 0.8465 | 0.1184 | ⚠️ Possible Overfit |
| CatBoost | 0.9758 ± 0.0162 | 0.8969 | 0.0789 | ✅ Healthy |
| SVM | 0.9626 ± 0.0109 | 0.9278 | 0.0348 | ✅ Healthy |
| KNN | 0.9118 ± 0.0283 | 0.8912 | 0.0206 | ✅ Healthy |

## Mitigation

- Stratified K-Fold (5-fold) prevents data leakage across folds.
- SMOTE applied only inside training folds.
- `class_weight='balanced'` combats minority class bias.
- Regularisation (max_depth, min_samples, L2) reduces overfitting.
