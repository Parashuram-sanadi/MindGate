"""
tests/test_batch_file.py
Integration test for CSV/Excel batch upload.
Verifies that various data types (int/str) are correctly handled.
"""

import sys, os
import pandas as pd
import io
import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

MODEL_READY = os.path.exists(os.path.join("models", "best_model.pkl"))

if MODEL_READY:
    from fastapi.testclient import TestClient
    from api.main import app
    client = TestClient(app)

@pytest.mark.skipif(not MODEL_READY, reason="Model not trained yet")
class TestBatchFileUpload:

    def test_csv_upload_mixed_types(self):
        # Create a small dataset with mixed types for Q26 and others
        data = {
            "Student ID": ["S1001", "S1002"],
            "Name": ["Alice", "Bob"],
            "Q1_AgeGroup": ["19-21", "22-25"],
            "Q2_Gender": ["Female", "Male"],
            "Q3_YearOfStudy": ["2nd Year", "3rd Year"],
            "Q4_Department": ["CSE", "ECE"],
            "Q5_Residence": ["Hostel", "Home"],
            "Q6_PrimaryGadget": ["Smartphone", "Laptop"],
            "Q7_ScreenTime": ["3-5", "5-7"],
            "Q8_GadgetUses": ["Social Media", "Gaming"],
            "Q9_SocialPlatforms": ["2-3", "4-5"],
            "Q11_PhoneChecks_PerHr": ["3-5", "6-10"],
            "Q15_GadgetFreeDay": ["Sometimes", "Never"],
            "sleep_hours": [7.0, 6.0],
            "Q23_SleepDuration": ["6-7", "5-6"],
            "Q24_GadgetBefore30minSleep": ["Often", "Always"],
            "Q25_WakesForPhone": ["Rarely", "Sometimes"],
            "Q26_SleepQuality": [4, "Often"], # Mixed type: Alice has int, Bob has str
            "Q27_ExercisePerWeek": ["1-3", 1], # Mixed type
            "Q28_PhysicalSymptoms": ["Eye strain", "Headaches"],
            "Q29_SkipsMeals": ["Sometimes", "Rarely"],
            "Q16_CGPA": ["7-8", "8-9"],
            "Q17_ClassesMissed": ["0-2", "3-5"],
            "Q18_GadgetDistractsStudy": [3, "3"], # Mixed type
            "Q19_SelfStudyHrs": ["2-3", "3-4"],
            "Q20_ProductivityRating": [3, 4],
            "Q21_GadgetHelpsAcademic": [3, 2],
            "Q22_AssignmentsOnTime": ["Usually", "Sometimes"],
            "Q13_AnxiousWithoutGadget": [3, 5],
            "Q30_StressLevel": [2, 4],
            "Q12_PhantomCheckUrge": ["Sometimes", "Often"],
            "Q33_EmotionalCoping": [3, 2],
            "Q34_UsageOutOfControl": [2, 4],
            "Q37_SelfLabel": ["No", "Maybe"],
            "Q38_SatisfactionRating": [4, 3],
            "Q36_TriedToReduce": ["Tried and failed", "Never tried"],
            "Q10_LateNightUsage": ["Sometimes", "Often"],
            "Q14_GadgetAtMeals": ["Sometimes", "Always"],
            "Q31_SocialInteractionQuality": [4, 3],
            "Q32_PrefersOnline": ["Rarely", "Sometimes"],
            "Q35_FeltNeedToReduce": ["Yes sometimes", "Yes often"]
        }

        df = pd.DataFrame(data)
        csv_buffer = io.BytesIO()
        df.to_csv(csv_buffer, index=False)
        csv_buffer.seek(0)

        files = {"file": ("test.csv", csv_buffer, "text/csv")}
        r = client.post("/predict/batch/file", files=files)

        assert r.status_code == 200
        resp_data = r.json()
        assert resp_data["summary"]["total_processed"] == 2
        assert resp_data["summary"]["total_skipped"] == 0
