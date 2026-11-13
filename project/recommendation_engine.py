"""
recommendation_engine.py
Rule-based recommendation engine for digital wellness interventions.
Returns personalized recommendations based on risk level and key SHAP factors.
"""

from typing import Optional, List, Dict

# ─── Tiered Base Recommendations ──────────────────────────────────────────────

BASE_RECOMMENDATIONS: Dict[str, List[str]] = {
    "Low": [
        "Great job maintaining a healthy and balanced digital lifestyle!",
        "Continue setting daily screen-free hours during meals and before sleep.",
        "Sustain your healthy habits with regular offline hobbies and physical activity.",
        "Track your weekly usage patterns to stay mindful of your digital balance.",
    ],
    "Moderate": [
        "Set daily app limits on your most frequently used applications.",
        "Establish a 30-minute screen-free routine before going to sleep.",
        "Clean up non-essential notifications to reduce frequent phone checks.",
        "Practice the 20-20-20 rule: every 20 minutes, look at something 20 feet away for 20 seconds.",
        "Schedule regular offline breaks and engage in physical or creative activities.",
    ],
    "High": [
        "Configure strict daily usage limits on your main problem applications.",
        "Enable bedtime or quiet-hours lock to protect your sleep schedule.",
        "Create phone-free zones during meals and dedicated study blocks.",
        "Connect a parent or guardian account for shared digital wellness accountability.",
        "Replace evening scrolling time with scheduled physical or social activities.",
        "Track your progress weekly to build consistent self-regulation habits.",
    ],
    "Severe": [
        "Set immediate strict daily limits and an automatic bedtime lock on your device.",
        "Involve a parent, guardian, or trusted mentor to support your wellness goals.",
        "Consider speaking with a school counselor or mental health professional for structured guidance.",
        "Establish complete phone-free zones in your bedroom and study area.",
        "Follow a gradual reduction plan, reducing screen time by 15–20% each week.",
        "Monitor key wellness indicators such as sleep quality, mood changes, and social connection.",
        "Support helpline for guidance: iCall – 9152987821.",
    ],
}

# ─── Factor-Specific Personalized Tips ────────────────────────────────────────

FACTOR_TIPS: Dict[str, str] = {
    # Sleep & Late Night
    "sleep_hours": "Your sleep is below recommended levels. Set a device curfew 1 hour before bedtime.",
    "Q23_SleepDuration": "Short sleep duration observed. Establish a consistent sleep schedule without devices.",
    "Q24_GadgetBefore30minSleep": "Using devices right before sleep reduces sleep quality. Turn off screens 30 minutes before bed.",
    "Q25_WakesForPhone": "Waking up to check your phone interrupts deep sleep. Place your phone across the room at night.",
    "Q10_LateNightUsage": "Late-night device use is a key factor. Keep your phone out of the bedroom overnight.",
    "late_night_usage": "Late-night device use is a key factor. Keep your phone out of the bedroom overnight.",

    # Usage & Phone Checks
    "Q7_ScreenTime": "Your total screen time is higher than average. Set daily app limits using MindGate.",
    "screen_time": "Your total screen time is higher than average. Set daily app limits using MindGate.",
    "Q11_PhoneChecks_PerHr": "Frequent phone checking interrupts focus. Mute non-urgent notifications during work or study.",
    "phone_checks": "Frequent phone checking interrupts focus. Mute non-urgent notifications during work or study.",
    "Q12_PhantomCheckUrge": "Urges to check your phone can be reduced by keeping the device out of sight while focusing.",

    # Stress & Psychological
    "Q30_StressLevel": "Higher stress levels observed. Short 5-minute breathing exercises or breaks can help lower stress.",
    "stress_level": "Higher stress levels observed. Short 5-minute breathing exercises or breaks can help lower stress.",
    "Q13_AnxiousWithoutGadget": "Feeling anxious without your device is common. Try short 15-minute phone-free intervals.",
    "Q34_UsageOutOfControl": "Setting firm app limits and sharing progress with a friend or family member helps regain control.",
    "Q33_EmotionalCoping": "Using devices to cope with stress can lead to overload. Try walking, music, or journaling instead.",

    # Academic & Productivity
    "Q17_ClassesMissed": "Device distraction may be affecting class attendance. Use focus mode during lecture hours.",
    "missed_classes": "Device distraction may be affecting class attendance. Use focus mode during lecture hours.",
    "Q18_GadgetDistractsStudy": "Device distractions impact study efficiency. Use structured study blocks (e.g. Pomodoro technique).",
    "Q20_ProductivityRating": "Low self-reported productivity. Set 45-minute focused study sessions without social media.",

    # Physical Health
    "Q28_PhysicalSymptoms": "Physical discomfort (eye strain or headaches) detected. Take a 5-minute break every 30 minutes.",
    "Q28_PhysicalSymptoms_count": "Frequent eye strain or headaches reported. Remember the 20-20-20 rule and take regular breaks.",
    "Q29_SkipsMeals": "Skipping meals due to device use affects energy levels. Keep meal times completely screen-free.",
}

# Target max items per tier
TIER_ITEM_COUNTS: Dict[str, int] = {
    "Low": 4,
    "Moderate": 5,
    "High": 6,
    "Severe": 7,
}

INTERVENTION_MAP: Dict[str, str] = {
    "Low": "Monitor",
    "Moderate": "Awareness",
    "High": "Intervention",
    "Severe": "Urgent Intervention",
}


# ─── Main Recommendation Function ────────────────────────────────────────────

def get_recommendations(
    risk_level: str,
    features: Optional[dict] = None,
    top_factors: Optional[list] = None,
    max_base: Optional[int] = None,
) -> dict:
    """
    Generate personalized recommendations based on risk level and top SHAP factors.

    Parameters
    ----------
    risk_level : str  – One of: Low, Moderate, High, Severe (case-insensitive).
    features   : dict – Optional user feature dict for fallback checks.
    top_factors: list – Top contributing feature names from SHAP engine.
    max_base   : int  – Optional override for item count limit.

    Returns
    -------
    dict:
        risk_level         : str
        recommendations    : list[str]
        factor_tips        : list[str]
        intervention_level : str
        primary_message     : str
    """
    normalized_level = (risk_level or "Moderate").strip().capitalize()
    if normalized_level not in BASE_RECOMMENDATIONS:
        normalized_level = "Moderate"

    target_count = max_base if max_base is not None else TIER_ITEM_COUNTS.get(normalized_level, 5)

    # 1. Identify personalized factor tips from top_factors
    factor_tips: List[str] = []
    if top_factors:
        for factor in top_factors:
            if factor in FACTOR_TIPS and FACTOR_TIPS[factor] not in factor_tips:
                factor_tips.append(FACTOR_TIPS[factor])

    # Fallback to features dict if no top_factors matched
    if not factor_tips and features:
        if features.get("sleep_hours", 8) < 6 and "sleep_hours" in FACTOR_TIPS:
            factor_tips.append(FACTOR_TIPS["sleep_hours"])
        if features.get("stress_level", 1) >= 4 and "stress_level" in FACTOR_TIPS:
            factor_tips.append(FACTOR_TIPS["stress_level"])

    # 2. Combine personalized items first, followed by tier items
    tier_items = BASE_RECOMMENDATIONS[normalized_level]
    combined = factor_tips + tier_items

    # 3. Deduplicate preserving order and trim to target item count
    final_recs = list(dict.fromkeys(combined))[:target_count]

    return {
        "risk_level":          normalized_level,
        "recommendations":     final_recs,
        "factor_tips":         factor_tips,
        "intervention_level":  INTERVENTION_MAP.get(normalized_level, "Awareness"),
        "primary_message":     final_recs[0] if final_recs else "",
    }


def get_primary_recommendation(risk_level: str) -> str:
    """Return single concise primary recommendation string for API response."""
    normalized_level = (risk_level or "Moderate").strip().capitalize()
    recs = BASE_RECOMMENDATIONS.get(normalized_level, BASE_RECOMMENDATIONS["Moderate"])
    return recs[0] if recs else "Maintain balanced screen habits."
