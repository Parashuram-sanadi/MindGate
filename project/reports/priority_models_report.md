# Priority Models Report

Highest-priority models for survey / behavioural data:
**CatBoost, Random Forest, XGBoost**

## Detailed Metrics

### Random Forest

| Metric | Value |
|--------|-------|
| CV Accuracy | 0.965 |
| CV Precision | 0.9668 |
| CV Recall | 0.9646 |
| CV F1 | 0.9648 ± 0.0128 |
| Test Accuracy | 0.9125 |
| Test F1 | 0.873 |
| Train Time | 0.212 s |
| Inference Time | 0.065775 s |

### XGBoost

| Metric | Value |
|--------|-------|
| CV Accuracy | 0.9584 |
| CV Precision | 0.9595 |
| CV Recall | 0.9581 |
| CV F1 | 0.9582 ± 0.0081 |
| Test Accuracy | 0.9125 |
| Test F1 | 0.873 |
| Train Time | 0.641 s |
| Inference Time | 0.005408 s |

### CatBoost

| Metric | Value |
|--------|-------|
| CV Accuracy | 0.9759 |
| CV Precision | 0.9768 |
| CV Recall | 0.9756 |
| CV F1 | 0.9758 ± 0.0162 |
| Test Accuracy | 0.925 |
| Test F1 | 0.8969 |
| Train Time | 0.376 s |
| Inference Time | 0.003874 s |

## Recommendation

**CatBoost** is the recommended production model for gadget addiction prediction:

- Natively handles categorical features without manual encoding overhead.
- Robust on small datasets (396 samples) due to ordered boosting.
- Minimal hyperparameter sensitivity – good default performance.
- Fastest inference among boosting models.
- Excellent SHAP compatibility for explainability.
