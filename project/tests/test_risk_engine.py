"""
tests/test_risk_engine.py
Unit tests for the risk engine and danger gate logic.
Run: python -m pytest tests/ -v
"""

import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from risk_engine import (
    score_to_band, validate_severe, apply_danger_gates, proba_to_risk
)


class TestScoreToBand:
    def test_low(self):
        assert score_to_band(10)  == "Low"
        assert score_to_band(0)   == "Low"
        assert score_to_band(34)  == "Low"

    def test_moderate(self):
        assert score_to_band(35)  == "Moderate"
        assert score_to_band(50)  == "Moderate"
        assert score_to_band(54)  == "Moderate"

    def test_high(self):
        assert score_to_band(55)  == "High"
        assert score_to_band(60)  == "High"
        assert score_to_band(64)  == "High"

    def test_severe(self):
        assert score_to_band(65)  == "Severe"
        assert score_to_band(80)  == "Severe"
        assert score_to_band(100) == "Severe"


class TestDangerGates:
    def test_all_gates_pass_returns_severe(self):
        result = validate_severe("Severe", True, True, True)
        assert result == "Severe"

    def test_gate1_fails_downgrades_to_high(self):
        result = validate_severe("Severe", False, True, True)
        assert result == "High"

    def test_gate2_fails_downgrades_to_high(self):
        result = validate_severe("Severe", True, False, True)
        assert result == "High"

    def test_gate3_fails_downgrades_to_high(self):
        result = validate_severe("Severe", True, True, False)
        assert result == "High"

    def test_all_gates_fail_downgrades_to_high(self):
        result = validate_severe("Severe", False, False, False)
        assert result == "High"

    def test_non_severe_unaffected(self):
        assert validate_severe("High",     True, True, True) == "High"
        assert validate_severe("Moderate", True, True, True) == "Moderate"
        assert validate_severe("Low",      True, True, True) == "Low"


class TestApplyDangerGates:
    def test_severe_confirmed(self):
        res = apply_danger_gates("Severe", gate1_score_valid=True,
                                 gate2_danger_met=True, gate3_loss_control=True)
        assert res["final_prediction"] == "Severe"
        assert res["downgraded"] is False
        assert res["gates_checked"] is True

    def test_severe_downgraded(self):
        res = apply_danger_gates("Severe", gate1_score_valid=True,
                                 gate2_danger_met=False, gate3_loss_control=True)
        assert res["final_prediction"] == "High"
        assert res["downgraded"] is True

    def test_from_row_data(self):
        row = {"gate1_score_valid": True, "gate2_danger_met": True, "gate3_loss_control": True}
        res = apply_danger_gates("Severe", row_data=row)
        assert res["final_prediction"] == "Severe"

    def test_non_severe_gates_not_checked(self):
        res = apply_danger_gates("High", gate1_score_valid=True,
                                 gate2_danger_met=True, gate3_loss_control=True)
        assert res["gates_checked"] is False
        assert res["final_prediction"] == "High"


class TestProbaToRisk:
    LABELS = ["Low", "Moderate", "High", "Severe"]

    def test_high_confidence_moderate(self):
        proba  = [0.05, 0.85, 0.05, 0.05]
        result = proba_to_risk(proba, self.LABELS)
        assert result["risk_level"] == "Moderate"
        assert result["confidence"] == 0.85

    def test_severe_downgraded_when_gates_absent(self):
        proba  = [0.01, 0.01, 0.05, 0.93]
        # No gates provided → defaults to False
        result = proba_to_risk(proba, self.LABELS, gate1=False, gate2=False, gate3=False)
        assert result["risk_level"] == "High"
        assert result["downgraded"] is True

    def test_severe_confirmed_when_gates_pass(self):
        proba  = [0.01, 0.01, 0.05, 0.93]
        result = proba_to_risk(proba, self.LABELS, gate1=True, gate2=True, gate3=True)
        assert result["risk_level"] == "Severe"
        assert result["downgraded"] is False
