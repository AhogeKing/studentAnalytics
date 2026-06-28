from __future__ import annotations

from collections.abc import Mapping
from typing import Any

import numpy as np
import pandas as pd

try:
    from .constants import BASE_FEATURE_COLUMNS, FEATURE_COLUMNS, TARGET_COLUMN
except ImportError:
    from constants import BASE_FEATURE_COLUMNS, FEATURE_COLUMNS, TARGET_COLUMN


def gpa_to_grade_class(gpa: float) -> int:
    if gpa >= 3.5:
        return 0
    if gpa >= 3.0:
        return 1
    if gpa >= 2.5:
        return 2
    if gpa >= 2.0:
        return 3
    return 4


def add_grade_class_clean(df: pd.DataFrame) -> pd.DataFrame:
    if "GPA" not in df.columns:
        raise ValueError("GPA column is required to build GradeClassClean.")

    result = df.copy()
    gpa = result["GPA"].astype(float)
    result[TARGET_COLUMN] = np.select(
        [
            gpa >= 3.5,
            gpa >= 3.0,
            gpa >= 2.5,
            gpa >= 2.0,
        ],
        [0, 1, 2, 3],
        default=4,
    ).astype(int)
    return result


def add_derived_features(df: pd.DataFrame) -> pd.DataFrame:
    required = {
        "Extracurricular",
        "Sports",
        "Music",
        "Volunteering",
        "StudyTimeWeekly",
        "Absences",
    }
    missing = sorted(required - set(df.columns))
    if missing:
        raise ValueError(f"Missing columns for derived features: {missing}")

    result = df.copy()
    result["ActivityCount"] = (
        result["Extracurricular"].astype(float)
        + result["Sports"].astype(float)
        + result["Music"].astype(float)
        + result["Volunteering"].astype(float)
    )
    result["StudyAbsenceRatio"] = (
        result["StudyTimeWeekly"].astype(float) / (result["Absences"].astype(float) + 1.0)
    )
    return result


def prepare_training_frame(df: pd.DataFrame) -> pd.DataFrame:
    return add_derived_features(add_grade_class_clean(df))


def prepare_prediction_frame(features: Mapping[str, Any] | pd.DataFrame) -> pd.DataFrame:
    if isinstance(features, pd.DataFrame):
        frame = features.copy()
    else:
        frame = pd.DataFrame([dict(features)])

    missing = sorted(set(BASE_FEATURE_COLUMNS) - set(frame.columns))
    if missing:
        raise ValueError(f"Missing prediction features: {missing}")

    for column in BASE_FEATURE_COLUMNS:
        frame[column] = pd.to_numeric(frame[column], errors="raise")

    frame = add_derived_features(frame)
    return frame[FEATURE_COLUMNS].copy()
