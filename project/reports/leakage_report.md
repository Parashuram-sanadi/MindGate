# Data Leakage Prevention Report
## Leakage Columns Found in Dataset
The following columns were identified as potential data leakage sources and have been **removed** from the training feature set:

- `Predicted_AddictionRisk`
- `computed_risk_score`
- `score_usage_compulsion`
- `score_sleep_physical`
- `score_academic_impairment`
- `score_emotional_dependence`
- `score_social_withdrawal`
- `score_loss_of_control`
- `danger_marker_count`
- `gate1_score_valid`
- `gate2_danger_met`
- `gate3_loss_control`
- `all_gates_pass`

**Total leakage columns removed:** 13

## Why These Columns Are Leakage

These columns are **derived from the target** (computed risk score, gate logic, sub-scores). Using them during training would inflate model performance and make the model useless in production where these values are unavailable at inference time.

## Columns Permitted ONLY For Post-Processing

| Column | Permitted Use |
|--------|---------------|
| `computed_risk_score` | Risk band validation, severity downgrade logic |
| `gate1_score_valid` | Severe risk verification |
| `gate2_danger_met` | Severe risk verification |
| `gate3_loss_control` | Severe risk verification |
| `all_gates_pass` | Final severity confirmation |
| `Predicted_AddictionRisk` | Evaluation / comparison only |

## Action Taken

All leakage columns stripped from `X_train`, `X_val`, and `X_test` before any model sees the data.
