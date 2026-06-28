from __future__ import annotations

from pathlib import Path
from typing import Final

PROJECT_ROOT: Final[Path] = Path(__file__).resolve().parents[2]
DEFAULT_DATA_PATH: Final[Path] = PROJECT_ROOT / "dataset" / "Student_performance_data_.csv"
DEFAULT_ARTIFACT_DIR: Final[Path] = Path(__file__).resolve().parent / "artifacts"
DEFAULT_MODEL_PATH: Final[Path] = DEFAULT_ARTIFACT_DIR / "grade_class_decision_tree.joblib"
DEFAULT_METRICS_PATH: Final[Path] = DEFAULT_ARTIFACT_DIR / "metrics.json"

RANDOM_STATE: Final[int] = 42
TEST_SIZE: Final[float] = 0.2
CV_FOLDS: Final[int] = 5

EXPECTED_COLUMNS: Final[list[str]] = [
    "StudentID",
    "Age",
    "Gender",
    "Ethnicity",
    "ParentalEducation",
    "StudyTimeWeekly",
    "Absences",
    "Tutoring",
    "ParentalSupport",
    "Extracurricular",
    "Sports",
    "Music",
    "Volunteering",
    "GPA",
    "GradeClass",
]

BASE_FEATURE_COLUMNS: Final[list[str]] = [
    "Age",
    "Gender",
    "Ethnicity",
    "ParentalEducation",
    "StudyTimeWeekly",
    "Absences",
    "Tutoring",
    "ParentalSupport",
    "Extracurricular",
    "Sports",
    "Music",
    "Volunteering",
]

DERIVED_FEATURE_COLUMNS: Final[list[str]] = [
    "ActivityCount",
    "StudyAbsenceRatio",
]

FEATURE_COLUMNS: Final[list[str]] = BASE_FEATURE_COLUMNS + DERIVED_FEATURE_COLUMNS
TARGET_COLUMN: Final[str] = "GradeClassClean"

NUMERIC_FEATURES: Final[list[str]] = [
    "Age",
    "StudyTimeWeekly",
    "Absences",
    "ActivityCount",
    "StudyAbsenceRatio",
]

ORDINAL_FEATURES: Final[list[str]] = [
    "Gender",
    "ParentalEducation",
    "Tutoring",
    "ParentalSupport",
    "Extracurricular",
    "Sports",
    "Music",
    "Volunteering",
]

ONE_HOT_FEATURES: Final[list[str]] = [
    "Ethnicity",
]

CLASS_LABELS: Final[dict[int, str]] = {
    0: "A",
    1: "B",
    2: "C",
    3: "D",
    4: "F",
}
CLASS_IDS: Final[list[int]] = list(CLASS_LABELS.keys())
