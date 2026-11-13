"""
tests/test_recommendation_engine.py
"""

import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from recommendation_engine import get_recommendations, get_primary_recommendation, BASE_RECOMMENDATIONS


class TestRecommendations:
    def test_returns_dict(self):
        res = get_recommendations("Low")
        assert isinstance(res, dict)
        assert "recommendations" in res
        assert "intervention_level" in res

    def test_all_risk_levels(self):
        for level in ["Low", "Moderate", "High", "Severe"]:
            res = get_recommendations(level)
            assert res["risk_level"] == level
            assert len(res["recommendations"]) > 0

    def test_intervention_levels(self):
        assert get_recommendations("Low")["intervention_level"]      == "Monitor"
        assert get_recommendations("Moderate")["intervention_level"] == "Awareness"
        assert get_recommendations("High")["intervention_level"]     == "Intervention"
        assert get_recommendations("Severe")["intervention_level"]   == "Urgent Intervention"

    def test_factor_tips_sleep(self):
        res = get_recommendations("High", features={"sleep_hours": 4})
        sleep_tips = [t for t in res["factor_tips"] if "sleep" in t.lower() or "curfew" in t.lower()]
        assert len(sleep_tips) > 0

    def test_primary_recommendation_returns_string(self):
        for level in ["Low", "Moderate", "High", "Severe"]:
            msg = get_primary_recommendation(level)
            assert isinstance(msg, str)
            assert len(msg) > 5

    def test_unknown_risk_level_defaults_to_moderate(self):
        res = get_recommendations("Unknown")
        assert res["risk_level"] == "Moderate"
