"""
tests/test_robust_ingestion.py
Verifies that the batch upload endpoint can handle the complex augmented dataset.
"""

import sys, os
import pandas as pd
import io
import pytest
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

MODEL_READY = os.path.exists(os.path.join("models", "best_model.pkl"))

if MODEL_READY:
    from fastapi.testclient import TestClient
    from api.main import app
    client = TestClient(app)

@pytest.mark.skipif(not MODEL_READY, reason="Model not trained yet")
class TestRobustIngestion:

    def test_augmented_dataset_format(self):
        # Create a mock 64-column dataset based on the user's description
        data = {
            "Name": ["Alice", "Bob"],
            "ID": [1001, 1002],
            "Q1_AgeGroup": ["19-21", "22-25"],
            "Q2_Gender": ["Female", "Male"],
            "Q3_YearOfStudy": ["2nd Year", "3rd Year"],
            "Q4_Department": ["CSE", "ECE"],
            "Q5_Residence": ["Hostel", "Home"],
            "Q6_PrimaryGadget": ["Smartphone", "Laptop"],
            "Q7_ScreenTime": ["3-5", "5-7"],
            "Q8_GadgetUses": ["Gaming,Studying", "Browsing"],
            "Q9_SocialPlatforms": ["2-3", "4-5"],
            "Q10_LateNightUsage": ["Sometimes", "Often"],
            "Q11_PhoneChecks_PerHr": ["3-5", "6-10"],
            "Q12_PhantomCheckUrge": ["Sometimes", "Often"],
            "Q13_AnxiousWithoutGadget": [3, 5],
            "Q14_GadgetAtMeals": ["Often", 1], # Mixed str/int
            "Q15_GadgetFreeDay": ["Sometimes", "Never"],
            "Q16_CGPA": ["7-8", "8-9"],
            "Q17_ClassesMissed": ["0-2", "3-5"],
            "Q18_GadgetDistractsStudy": [3, 4],
            "Q19_SelfStudyHrs": ["2-3", "3-4"],
            "Q20_ProductivityRating": [3, 4],
            "Q21_GadgetHelpsAcademic": [3, 2],
            "Q22_AssignmentsOnTime": ["Usually", "Sometimes"],
            "Q23_SleepDuration": ["6-7", "5-6"],
            "Q24_GadgetBefore30minSleep": ["Often", "Always"],
            "Q25_WakesForPhone": ["Rarely", "Sometimes"],
            "Q26_SleepQuality": [4, "Always"], # Mixed int/str
            "Q27_ExercisePerWeek": ["1-3", 1], # Mixed str/int
            "Q28_PhysicalSymptoms": ["Back pain", np.nan], # Has null
            "Q29_SkipsMeals": ["Sometimes", "Rarely"],
            "Q30_StressLevel": [2, 4],
            "Q31_SocialInteractionQuality": [4, 3],
            "Q32_PrefersOnline": ["Rarely", 5], # Mixed str/int
            "Q33_EmotionalCoping": [3, 2],
            "Q34_UsageOutOfControl": [2, 4],
            "Q35_FeltNeedToReduce": ["Yes sometimes", "Yes often"],
            "Q36_TriedToReduce": ["Tried and failed", "Never tried"],
            "Q37_SelfLabel": ["No", "Maybe"],
            "Q38_SatisfactionRating": [4, 3],
            "sleep_hours": [7.0, 6.0]
        }

        # Add 23 extra columns (total columns 42-64) to reach 64 columns
        for i in range(42, 65):
            col_name = f"extra_col_{i}"
            if i == 62: col_name = "Predicted_AddictionRisk"
            if i == 63: col_name = "Corrected_Label"
            if i == 64: col_name = "Scenario_Note"
            data[col_name] = ["extra_val"] * 2

        df = pd.DataFrame(data)

        # Verify shape
        assert df.shape[1] == 64

        csv_buffer = io.BytesIO()
        df.to_csv(csv_buffer, index=False)
        csv_buffer.seek(0)

        files = {"file": ("dataset.csv", csv_buffer, "text/csv")}
        r = client.post("/predict/batch/file", files=files)

        assert r.status_code == 200
        resp_data = r.json()
        assert resp_data["summary"]["total_processed"] == 2
        assert resp_data["summary"]["total_skipped"] == 0

        # Verify extra columns were ignored in prediction logic but kept in student_info
        # except for the labels
        first_result = resp_data["results"][0]
        assert "Predicted_AddictionRisk" not in first_result["student_info"]
        assert "Corrected_Label" not in first_result["student_info"]
        assert "extra_col_42" in first_result["student_info"]
