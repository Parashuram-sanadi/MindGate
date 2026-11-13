"""
src/feature_derivation.py
Derives correlated/redundant feature values from the core user-supplied
inputs so that minimal API requests produce realistic predictions.

The raw dataset contains several pairs of features that encode the SAME
underlying signal in different forms (e.g. `Q7_ScreenTime` band vs
`daily_screen_time_hours` numeric). When a user supplies only the
banded/categorical value via the API, we derive the numeric counterpart
(and vice-versa) so the model receives an internally consistent feature
vector instead of conflicting defaults.
"""

import numpy as np


# Mean numeric hours per Q7_ScreenTime band (derived from training data)
SCREEN_TIME_TO_HOURS = {
    "<2":  1.7,
    "2-3": 2.3,
    "3-5": 3.9,
    "5-7": 5.5,
    "7-9": 8.8,
    "9+":  9.8,
}

# Mean late_night_usage numeric score per Q10_LateNightUsage category
LATE_NIGHT_TO_NUMERIC = {
    "Never":     0.4,
    "Rarely":    0.8,
    "Sometimes": 1.4,
    "Often":     2.6,
    "Always":    3.5,
}

# Approximate sleep_disturbances given reported sleep_hours
def _sleep_hours_to_disturbances(hours: float) -> float:
    if hours >= 8:
        return 0.3
    if hours >= 7:
        return 0.5
    if hours >= 6:
        return 1.7
    if hours >= 5:
        return 2.5
    return 3.5


# Approximate Q26_SleepQuality (1=poor .. 5=excellent) given sleep_hours
def _sleep_hours_to_quality(hours: float) -> int:
    if hours >= 8:
        return 5
    if hours >= 7:
        return 4
    if hours >= 6:
        return 3
    if hours >= 5:
        return 2
    return 1


def derive_fields(record: dict) -> dict:
    """
    Given a (partial) record containing core user inputs, derive
    correlated numeric/categorical fields. Returns a dict of ONLY
    the derived fields (to be merged on top of defaults, but
    BELOW any explicitly user-supplied values).
    """
    derived = {}

    # Screen time band -> numeric hours
    screen_band = record.get("Q7_ScreenTime")
    if screen_band in SCREEN_TIME_TO_HOURS:
        derived["daily_screen_time_hours"] = SCREEN_TIME_TO_HOURS[screen_band]

    # Late night usage category -> numeric score
    late_night = record.get("Q10_LateNightUsage")
    if late_night in LATE_NIGHT_TO_NUMERIC:
        derived["late_night_usage"] = LATE_NIGHT_TO_NUMERIC[late_night]

    # Sleep hours -> disturbances + quality
    sleep_hours = record.get("sleep_hours")
    if sleep_hours is not None:
        try:
            sh = float(sleep_hours)
            derived["sleep_disturbances"] = _sleep_hours_to_disturbances(sh)
            derived["Q26_SleepQuality"]   = _sleep_hours_to_quality(sh)
        except (ValueError, TypeError):
            pass

    return derived


def build_full_record(user_fields: dict, defaults: dict) -> dict:
    """
    Compose the final feature record for prediction:
      1. Start from dataset-derived defaults (median/mode for all 44 cols).
      2. Overlay correlated fields derived from user inputs.
      3. Overlay explicit user-supplied fields (highest priority).

    This ordering ensures derived values fill gaps without overriding
    anything the user explicitly specified.
    """
    derived = derive_fields(user_fields)
    full = {**defaults, **derived, **user_fields}
    return full
