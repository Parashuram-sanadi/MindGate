"""
risk_engine.py
Production-grade risk scoring engine.
Implements danger gate validation and score-based risk banding.
"""

from typing import Optional


# ─── Risk Band Configuration ──────────────────────────────────────────────────

RISK_BANDS = {
    "Low":      (0,  34),
    "Moderate": (35, 54),
    "High":     (55, 64),
    "Severe":   (65, 999),
}

LABEL_TO_INT = {"Low": 0, "Moderate": 1, "High": 2, "Severe": 3}
INT_TO_LABEL = {v: k for k, v in LABEL_TO_INT.items()}


# ─── Risk Score → Band ────────────────────────────────────────────────────────

def score_to_band(score: float) -> str:
    """Map a numeric risk score to its risk band label."""
    for label, (lo, hi) in RISK_BANDS.items():
        if lo <= score <= hi:
            return label
    return "Severe"   # anything above 999 maps here


# ─── Danger Gate Validation ───────────────────────────────────────────────────

def derive_gates(raw_data: dict, risk_score: float) -> dict:
    """
    Calculate danger gates from raw survey responses if not explicitly provided.

    Logic:
    - Gate 1: risk_score >= 65
    - Gate 2: >= 2 danger markers (Physical symptoms, meal skipping, sleep wake-ups, phantom urge)
    - Gate 3: Explicit loss of control (Usage out of control OR tried & failed to reduce)
    """
    # Gate 1: Score check
    g1 = risk_score >= 65.0

    # Gate 2: Danger Markers
    markers = 0

    # physical symptoms
    symptoms = str(raw_data.get("Q28_PhysicalSymptoms", "")).lower()
    if any(s in symptoms for s in ["pain", "insomnia", "headache", "strain", "blur"]):
        markers += 1

    # habits
    if raw_data.get("Q29_SkipsMeals") in ["Always", "Often"]: markers += 1
    if raw_data.get("Q25_WakesForPhone") in ["Always", "Often"]: markers += 1
    if raw_data.get("Q12_PhantomCheckUrge") in ["Always", "Often"]: markers += 1

    g2 = markers >= 2

    # Gate 3: Loss of Control
    try:
        control_rating = float(raw_data.get("Q34_UsageOutOfControl", 0))
    except (ValueError, TypeError):
        control_rating = 0

    tried_failed = raw_data.get("Q36_TriedToReduce") == "Tried and failed"

    g3 = (control_rating >= 4) or tried_failed

    return {"gate1": g1, "gate2": g2, "gate3": g3}


def validate_severe(
    predicted_label: str,
    gate1_score_valid: bool,
    gate2_danger_met:  bool,
    gate3_loss_control: bool,
) -> str:
    """
    DANGER GATE LOGIC (Production Rule):

    Severe risk is confirmed ONLY when ALL three gates pass:
      gate1_score_valid  – Computed risk score >= 65
      gate2_danger_met   – At least 2 active danger markers present
      gate3_loss_control – Student explicitly reports loss of control

    If score >= 65 but gates fail → downgrade to High.

    Parameters
    ----------
    predicted_label     : str  – Model's predicted risk class.
    gate1_score_valid   : bool – Whether the risk score threshold is met.
    gate2_danger_met    : bool – Whether danger marker threshold is met.
    gate3_loss_control  : bool – Whether loss-of-control criterion is met.

    Returns
    -------
    str – Final validated risk label.
    """
    if predicted_label != "Severe":
        return predicted_label   # gates only apply to Severe

    if gate1_score_valid and gate2_danger_met and gate3_loss_control:
        return "Severe"
    else:
        return "High"            # downgrade


def apply_danger_gates(
    predicted_label: str,
    row_data: Optional[dict] = None,
    gate1_score_valid: Optional[bool] = None,
    gate2_danger_met:  Optional[bool] = None,
    gate3_loss_control: Optional[bool] = None,
    risk_score: float = 0.0,
    raw_data: Optional[dict] = None,
) -> dict:
    """
    Unified gate-check interface.

    Accepts either a dict (from dataset row) or individual gate booleans.
    If gates are missing, they are derived from raw_data.

    Returns
    -------
    dict with keys:
        raw_prediction    : str
        final_prediction  : str
        gates_checked     : bool
        gate_results      : dict
        downgraded        : bool
    """
    # 1. Start with provided values
    g1 = gate1_score_valid
    g2 = gate2_danger_met
    g3 = gate3_loss_control

    if row_data:
        if g1 is None: g1 = bool(row_data.get("gate1_score_valid", False))
        if g2 is None: g2 = bool(row_data.get("gate2_danger_met",  False))
        if g3 is None: g3 = bool(row_data.get("gate3_loss_control",False))

    # 2. Derive if any are missing and raw_data is available
    if raw_data and (g1 is None or g2 is None or g3 is None):
        derived = derive_gates(raw_data, risk_score)
        if g1 is None: g1 = derived["gate1"]
        if g2 is None: g2 = derived["gate2"]
        if g3 is None: g3 = derived["gate3"]

    # 3. Final Fallback to False
    g1 = bool(g1)
    g2 = bool(g2)
    g3 = bool(g3)

    final      = validate_severe(predicted_label, g1, g2, g3)
    downgraded = (predicted_label == "Severe") and (final == "High")

    return {
        "raw_prediction":  predicted_label,
        "final_prediction": final,
        "gates_checked":   (predicted_label == "Severe"),
        "gate_results": {
            "gate1_score_valid":  g1,
            "gate2_danger_met":   g2,
            "gate3_loss_control": g3,
            "all_gates_pass":     g1 and g2 and g3,
        },
        "downgraded": downgraded,
    }


# ─── Model Probability → Risk Level ──────────────────────────────────────────

def proba_to_risk(
    proba: list,
    label_order: list,
    gate1: Optional[bool] = None,
    gate2: Optional[bool] = None,
    gate3: Optional[bool] = None,
    raw_data: Optional[dict] = None,
) -> dict:
    """
    Convert model probability array to risk decision using a continuous formula.

    Formula:
    Score = (P(Low)*15 + P(Moderate)*45 + P(High)*70 + P(Severe)*90)
    This creates a smooth transition between 0 and 100 based on model confidence.

    Parameters
    ----------
    proba        : list/array – Probability for each class.
    label_order  : list       – Class label names in order matching proba.
    gate1/2/3    : bool       – Gate values (if available).
    raw_data     : dict       – Raw survey features for gate derivation.

    Returns
    -------
    dict with risk_level, risk_score, confidence, downgraded.
    """
    # 1. Calculate weighted score (0-100)
    try:
        p_low = proba[label_order.index("Low")]
        p_mod = proba[label_order.index("Moderate")]
        p_high = proba[label_order.index("High")]
        p_sev = proba[label_order.index("Severe")]
    except (ValueError, IndexError):
        p_low, p_mod, p_high, p_sev = 0, 0, 0, 0

    raw_score = (p_low * 15) + (p_mod * 45) + (p_high * 70) + (p_sev * 90)
    raw_score = round(float(raw_score), 2)

    # 2. Determine raw label from score threshold rather than just max probability
    # This ensures consistency with the 65+ threshold for Severe.
    raw_label = score_to_band(raw_score)

    # Also get max prob index for confidence reporting
    idx = int(max(range(len(proba)), key=lambda i: proba[i]))
    confidence = round(float(proba[idx]), 4)

    # 3. Apply gate logic
    result = apply_danger_gates(
        raw_label,
        gate1_score_valid=gate1,
        gate2_danger_met=gate2,
        gate3_loss_control=gate3,
        risk_score=raw_score,
        raw_data=raw_data
    )
    final_label = result["final_prediction"]

    # Final score adjustment if downgraded
    final_score = raw_score
    if result["downgraded"]:
        # If downgraded from Severe to High, cap score at 64
        final_score = min(raw_score, 64.0)

    return {
        "risk_level":     final_label,
        "risk_score":     final_score,
        "confidence":     confidence,
        "raw_prediction": raw_label,
        "downgraded":     result["downgraded"],
        "gate_results":   result["gate_results"],
    }
