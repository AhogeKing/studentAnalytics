from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib
from sklearn.model_selection import GridSearchCV, StratifiedKFold, train_test_split

try:
    from .constants import (
        CLASS_LABELS,
        CV_FOLDS,
        DEFAULT_ARTIFACT_DIR,
        DEFAULT_DATA_PATH,
        DEFAULT_METRICS_PATH,
        DEFAULT_MODEL_PATH,
        FEATURE_COLUMNS,
        RANDOM_STATE,
        TARGET_COLUMN,
        TEST_SIZE,
    )
    from .dataset import build_training_xy, load_student_performance_csv, summarize_label_noise
    from .evaluate import evaluate_classifier
    from .preprocess import build_pipeline, default_param_grid, exhaustive_param_grid, smoke_param_grid
except ImportError:
    from constants import (
        CLASS_LABELS,
        CV_FOLDS,
        DEFAULT_ARTIFACT_DIR,
        DEFAULT_DATA_PATH,
        DEFAULT_METRICS_PATH,
        DEFAULT_MODEL_PATH,
        FEATURE_COLUMNS,
        RANDOM_STATE,
        TARGET_COLUMN,
        TEST_SIZE,
    )
    from dataset import build_training_xy, load_student_performance_csv, summarize_label_noise
    from evaluate import evaluate_classifier
    from preprocess import build_pipeline, default_param_grid, exhaustive_param_grid, smoke_param_grid


def train_decision_tree(
    data_path: str | Path = DEFAULT_DATA_PATH,
    artifact_dir: str | Path = DEFAULT_ARTIFACT_DIR,
    quick: bool = False,
    exhaustive: bool = False,
) -> dict[str, Any]:
    artifact_path = Path(artifact_dir)
    artifact_path.mkdir(parents=True, exist_ok=True)

    model_path = artifact_path / DEFAULT_MODEL_PATH.name
    metrics_path = artifact_path / DEFAULT_METRICS_PATH.name

    df = load_student_performance_csv(data_path)
    X, y = build_training_xy(df)
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=y,
    )

    cv = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=RANDOM_STATE)
    grid = GridSearchCV(
        estimator=build_pipeline(),
        param_grid=choose_param_grid(quick=quick, exhaustive=exhaustive),
        scoring="f1_macro",
        cv=cv,
        n_jobs=-1,
        refit=True,
        verbose=1,
    )
    grid.fit(X_train, y_train)

    best_model = grid.best_estimator_
    test_metrics = evaluate_classifier(best_model, X_test, y_test)

    metrics = {
        "model_name": "grade_class_decision_tree",
        "algorithm": "DecisionTreeClassifier",
        "created_at": datetime.now(timezone.utc).isoformat(),
        "data_path": str(Path(data_path).resolve()),
        "model_path": str(model_path.resolve()),
        "target_column": TARGET_COLUMN,
        "feature_columns": FEATURE_COLUMNS,
        "class_labels": {str(key): value for key, value in CLASS_LABELS.items()},
        "train_rows": int(len(X_train)),
        "test_rows": int(len(X_test)),
        "label_noise": summarize_label_noise(df),
        "best_parameters": grid.best_params_,
        "search_mode": search_mode(quick=quick, exhaustive=exhaustive),
        "search_candidates": int(len(grid.cv_results_["params"])),
        "cv_best_f1_macro": float(grid.best_score_),
        "train_class_distribution": class_distribution(y_train),
        "test_class_distribution": class_distribution(y_test),
        **test_metrics,
    }

    joblib.dump(best_model, model_path)
    metrics_path.write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")

    return {
        "model_path": str(model_path),
        "metrics_path": str(metrics_path),
        "metrics": metrics,
    }


def class_distribution(y) -> dict[str, int]:
    counts = y.value_counts().sort_index()
    return {str(int(class_id)): int(count) for class_id, count in counts.items()}


def choose_param_grid(quick: bool, exhaustive: bool) -> dict[str, list[object]]:
    if quick and exhaustive:
        raise ValueError("--quick and --exhaustive cannot be used together.")
    if quick:
        return smoke_param_grid()
    if exhaustive:
        return exhaustive_param_grid()
    return default_param_grid()


def search_mode(quick: bool, exhaustive: bool) -> str:
    if quick:
        return "quick"
    if exhaustive:
        return "exhaustive"
    return "default"


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train GradeClassClean decision tree model.")
    parser.add_argument(
        "--data-path",
        type=Path,
        default=DEFAULT_DATA_PATH,
        help="CSV input path. Defaults to dataset/Student_performance_data_.csv.",
    )
    parser.add_argument(
        "--artifact-dir",
        type=Path,
        default=DEFAULT_ARTIFACT_DIR,
        help="Directory for model and metrics artifacts.",
    )
    parser.add_argument(
        "--quick",
        action="store_true",
        help="Use the 96-candidate local retraining grid.",
    )
    parser.add_argument(
        "--exhaustive",
        action="store_true",
        help="Use the original large parameter grid. This can be slow on local laptops.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    result = train_decision_tree(
        data_path=args.data_path,
        artifact_dir=args.artifact_dir,
        quick=args.quick,
        exhaustive=args.exhaustive,
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
