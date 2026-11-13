# MindGate AI Model Details

This document explains how to monitor the AI model's performance and the reasoning behind the selection of the CatBoost algorithm.

## 1. How to See Model Accuracy Details

The model performance is tracked and saved automatically. You can view detailed results in the following files within the `project/` directory:

*   **Best Model Summary:** `reports/best_model_report.md`
    *   Provides the final accuracy, F1-score, and cross-validation results.
*   **Per-Class Metrics:** `reports/classification_report.txt`
    *   Shows Precision, Recall, and F1-score for every risk level (Low, Moderate, High, Severe).
*   **Visual Confusion Matrix:** `plots/confusion_matrix_best_model.png`
    *   Shows exactly which risk categories the model predicts correctly and where it makes errors.

## 2. Command to View Accuracy

You can trigger a fresh evaluation of the current model to see accuracy in the terminal by running:

```powershell
python evaluate.py
```

**What this command does:**
1.  Loads the best-performing model from the `models/` folder.
2.  Processes the test data through the feature pipeline.
3.  Calculates accuracy, balanced accuracy, and macro F1-score.
4.  Generates and saves updated reports and plots.

## 3. Why CatBoost was Selected

After rigorous testing against other algorithms (Random Forest, SVM, LightGBM, Extra Trees), **CatBoost** was selected as the core risk engine for MindGate for several key reasons:

1.  **Native Categorical Support:** The gadget addiction dataset consists mostly of survey responses (strings/categories). CatBoost handles these natively using a specialized algorithm that prevents "data leakage" and performs better than standard encoding techniques.
2.  **Robustness to Small Data:** Since the training set is derived from a limited number of survey responses (~400), CatBoost's "Ordered Boosting" approach is superior at preventing overfitting compared to other gradient boosting frameworks.
3.  **Highest Performance Metrics:** In our leaderboard evaluation, CatBoost consistently achieved the highest **Cross-Validation F1 Mean (0.9758)** and **Accuracy (0.9759)**, making it the most reliable choice for real-world classification.
4.  **Explainability (SHAP):** It provides excellent support for SHAP values, which allows MindGate to provide "Top 3 Risk Factors" to teachers, explaining exactly *why* a student was flagged as high risk.

---
*Generated for the MindGate Digital Wellbeing Project.*
