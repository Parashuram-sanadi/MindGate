# Explainability Report (SHAP)

## Model: CatBoost

## Method

SHAP (SHapley Additive exPlanations) quantifies each feature's contribution to individual predictions. TreeExplainer is used for tree-based models, providing exact Shapley values in polynomial time.

## Top 20 Most Influential Features

| Rank | Feature | Mean |SHAP| |
|------|---------|----------------|
| 1 | `late_night_usage` | 0.2823 |
| 2 | `sleep_disturbances` | 0.1636 |
| 3 | `daily_screen_time_hours` | 0.1632 |
| 4 | `sleep_hours` | 0.1588 |
| 5 | `missed_classes_per_month` | 0.1274 |
| 6 | `Q30_StressLevel` | 0.1270 |
| 7 | `Q8_GadgetUses_count` | 0.1198 |
| 8 | `Q28_PhysicalSymptoms_count` | 0.1136 |
| 9 | `Q7_ScreenTime` | 0.1109 |
| 10 | `social_interaction_quality` | 0.0989 |
| 11 | `Q11_PhoneChecks_PerHr` | 0.0959 |
| 12 | `Q13_AnxiousWithoutGadget` | 0.0921 |
| 13 | `Q9_SocialPlatforms` | 0.0809 |
| 14 | `Q31_SocialInteractionQuality` | 0.0799 |
| 15 | `Q16_CGPA` | 0.0770 |
| 16 | `Q19_SelfStudyHrs` | 0.0662 |
| 17 | `Q38_SatisfactionRating` | 0.0609 |
| 18 | `Q24_GadgetBefore30minSleep` | 0.0606 |
| 19 | `Q36_TriedToReduce` | 0.0587 |
| 20 | `Q20_ProductivityRating` | 0.0531 |

## Interpretation

- **High |SHAP|** → feature strongly drives the prediction.
- **Positive SHAP** → pushes prediction toward higher risk.
- **Negative SHAP** → pushes prediction toward lower risk.

## Key Findings

The top contributing features for addiction risk prediction (this run) were:
1. `late_night_usage`
2. `sleep_disturbances`
3. `daily_screen_time_hours`
4. `sleep_hours`
5. `missed_classes_per_month`

These align with the clinical literature on problematic gadget use: late-night usage and sleep disruption are consistently the strongest predictors, followed by daily screen time, physical symptoms (eye strain, headaches, poor posture), missed classes, and phone-checking frequency.

See `plots/` directory for visual SHAP outputs (per-class summary plots, dependence plots, and local explanations).
