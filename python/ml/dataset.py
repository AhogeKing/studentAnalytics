from __future__ import annotations

from pathlib import Path
from typing import Any

import pandas as pd

try:
    from .constants import DEFAULT_DATA_PATH, EXPECTED_COLUMNS, FEATURE_COLUMNS, TARGET_COLUMN
    from .feature_engineering import prepare_training_frame
except ImportError:
    from constants import DEFAULT_DATA_PATH, EXPECTED_COLUMNS, FEATURE_COLUMNS, TARGET_COLUMN
    from feature_engineering import prepare_training_frame


def load_student_performance_csv(csv_path: str | Path = DEFAULT_DATA_PATH) -> pd.DataFrame:
    path = Path(csv_path)
    if not path.exists():
        raise FileNotFoundError(f"CSV file does not exist: {path.resolve()}")

    df = pd.read_csv(path)
    validate_columns(df)
    convert_numeric_columns(df)
    return df


def validate_columns(df: pd.DataFrame) -> None:
    columns = list(df.columns)
    missing = sorted(set(EXPECTED_COLUMNS) - set(columns))
    unknown = sorted(set(columns) - set(EXPECTED_COLUMNS))
    if missing:
        raise ValueError(f"CSV is missing columns: {missing}")
    if unknown:
        raise ValueError(f"CSV has unknown columns: {unknown}")
    if columns != EXPECTED_COLUMNS:
        raise ValueError("CSV column order does not match EXPECTED_COLUMNS.")


def convert_numeric_columns(df: pd.DataFrame) -> None:
    for column in EXPECTED_COLUMNS:
        df[column] = pd.to_numeric(df[column], errors="raise")

    integer_columns = [column for column in EXPECTED_COLUMNS if column not in {"GPA", "StudyTimeWeekly"}]
    for column in integer_columns:
        values = df[column].dropna()
        if not (values == values.astype(int)).all():
            raise ValueError(f"{column} should contain integer values only.")
        df[column] = df[column].astype(int)


def build_training_xy(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.Series]:
    model_frame = prepare_training_frame(df)
    return model_frame[FEATURE_COLUMNS].copy(), model_frame[TARGET_COLUMN].copy()


def summarize_label_noise(df: pd.DataFrame) -> dict[str, Any]:
    model_frame = prepare_training_frame(df)
    raw_grade_class = model_frame["GradeClass"].astype(int)
    clean_grade_class = model_frame[TARGET_COLUMN].astype(int)
    mismatch_mask = raw_grade_class != clean_grade_class
    mismatch_count = int(mismatch_mask.sum())

    summary: dict[str, Any] = {
        "source_target_column": "GradeClass",
        "clean_target_column": TARGET_COLUMN,
        "mismatch_count": mismatch_count,
        "total_rows": int(len(model_frame)),
        "mismatch_ratio": float(mismatch_count / len(model_frame)) if len(model_frame) else 0.0,
    }

    if "StudentID" in model_frame.columns and mismatch_count:
        mismatch_ids = model_frame.loc[mismatch_mask, "StudentID"].astype(int)
        summary["first_mismatch_student_id"] = int(mismatch_ids.min())
        summary["last_mismatch_student_id"] = int(mismatch_ids.max())

    return summary
