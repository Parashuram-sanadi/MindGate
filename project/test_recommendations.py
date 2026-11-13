"""
test_recommendations.py
Test script to verify tiered and personalized recommendations across all risk levels.
"""

import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from recommendation_engine import get_recommendations

def test_tiered_recommendations():
    print("\n=======================================================")
    print("      MINDGATE RECOMMENDATION ENGINE TEST SUITE       ")
    print("=======================================================\n")

    test_cases = [
        {
            "level": "Low",
            "top_factors": ["Q15_GadgetFreeDay", "Q27_ExercisePerWeek"],
            "desc": "Healthy baseline student"
        },
        {
            "level": "Moderate",
            "top_factors": ["Q11_PhoneChecks_PerHr", "Q10_LateNightUsage"],
            "desc": "Frequent phone checking + late night usage"
        },
        {
            "level": "High",
            "top_factors": ["Q7_ScreenTime", "Q18_GadgetDistractsStudy", "Q30_StressLevel"],
            "desc": "High screen time + study distraction + stress"
        },
        {
            "level": "Severe",
            "top_factors": ["Q10_LateNightUsage", "Q34_UsageOutOfControl", "Q17_ClassesMissed"],
            "desc": "Late night + loss of control + missed classes"
        }
    ]

    for tc in test_cases:
        level = tc["level"]
        factors = tc["top_factors"]
        desc = tc["desc"]

        result = get_recommendations(risk_level=level, top_factors=factors)

        print(f"--- TIER: {level.upper()} ({desc}) ---")
        print(f"Intervention Level : {result['intervention_level']}")
        print(f"Primary Message    : {result['primary_message']}")
        print(f"Personalized Tips  : {result['factor_tips']}")
        print(f"Total Recs Count   : {len(result['recommendations'])}")
        print("Full Recommendations List:")
        for idx, rec in enumerate(result['recommendations'], 1):
            print(f"  {idx}. {rec}")
        print("\n" + "-"*55 + "\n")

if __name__ == "__main__":
    test_tiered_recommendations()
