from __future__ import annotations

import argparse
import json
import sys
from collections.abc import Mapping
from pathlib import Path
from typing import Any

import joblib
import numpy as np

try:
    from .constants import CLASS_IDS, CLASS_LABELS, DEFAULT_MODEL_PATH
    from .evaluate import align_probabilities
    from .feature_engineering import prepare_prediction_frame
except ImportError:
    from constants import CLASS_IDS, CLASS_LABELS, DEFAULT_MODEL_PATH
    from evaluate import align_probabilities
    from feature_engineering import prepare_prediction_frame


SAMPLE_STUDENT: dict[str, int | float] = {
    "Age": 17,
    "Gender": 1,
    "Ethnicity": 0,
    "ParentalEducation": 2,
    "StudyTimeWeekly": 12.5,
    "Absences": 8,
    "Tutoring": 1,
    "ParentalSupport": 3,
    "Extracurricular": 1,
    "Sports": 0,
    "Music": 1,
    "Volunteering": 0,
}


def load_model(model_path: str | Path = DEFAULT_MODEL_PATH) -> Any:
    path = Path(model_path)
    if not path.exists():
        raise FileNotFoundError(
            f"Model file does not exist: {path.resolve()}. Run train_decision.py first."
        )
    return joblib.load(path)


def predict_one(
    features: Mapping[str, Any],
    model_path: str | Path = DEFAULT_MODEL_PATH,
) -> dict[str, Any]:
    model = load_model(model_path)
    row = prepare_prediction_frame(features)
    probabilities = align_probabilities(model, model.predict_proba(row))[0]
    predicted_class = int(CLASS_IDS[int(np.argmax(probabilities))])

    return {
        "predicted_grade_class": predicted_class,
        "predicted_grade_label": CLASS_LABELS[predicted_class],
        "probabilities": {
            CLASS_LABELS[class_id]: float(probabilities[index])
            for index, class_id in enumerate(CLASS_IDS)
        },
        "important_features": top_important_features(model),
        "model_path": str(Path(model_path).resolve()),
    }


def top_important_features(model: Any, limit: int = 5) -> list[dict[str, float | str]]:
    preprocessor = model.named_steps["preprocessor"]
    classifier = model.named_steps["model"]
    names = preprocessor.get_feature_names_out()
    importances = classifier.feature_importances_
    order = np.argsort(importances)[::-1]

    result: list[dict[str, float | str]] = []
    for index in order:
        importance = float(importances[index])
        if importance <= 0:
            continue
        result.append(
            {
                "feature": clean_feature_name(str(names[index])),
                "importance": importance,
            }
        )
        if len(result) >= limit:
            break
    return result


def clean_feature_name(name: str) -> str:
    if "__" in name:
        name = name.split("__", 1)[1]
    return name


def parse_feature_input(args: argparse.Namespace) -> dict[str, Any]:
    if args.sample:
        return SAMPLE_STUDENT
    if args.input:
        return json.loads(args.input.read_text(encoding="utf-8"))
    if args.json:
        return json.loads(args.json)
    raise ValueError("Provide --sample, --input, or --json.")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Predict one student's grade class.")
    parser.add_argument(
        "--model-path",
        type=Path,
        default=DEFAULT_MODEL_PATH,
        help="Path to grade_class_decision_tree.joblib.",
    )
    parser.add_argument("--sample", action="store_true", help="Use a built-in sample student.")
    parser.add_argument("--input", type=Path, help="Path to a JSON file containing one student.")
    parser.add_argument("--json", help="Inline JSON object containing one student.")
    parser.add_argument(
        "--output",
        type=Path,
        help="Path to write the prediction result JSON. Defaults to stdout.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    features = parse_feature_input(args)
    result = predict_one(features, model_path=args.model_path)
    output_text = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output_text + "\n", encoding="utf-8")
    else:
        print(output_text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
