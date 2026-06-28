from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)

try:
    from .constants import CLASS_IDS, CLASS_LABELS
except ImportError:
    from constants import CLASS_IDS, CLASS_LABELS


def evaluate_classifier(model: Any, X_test: pd.DataFrame, y_test: pd.Series) -> dict[str, Any]:
    y_pred = model.predict(X_test)
    y_prob = align_probabilities(model, model.predict_proba(X_test))

    metrics: dict[str, Any] = {
        "accuracy": float(accuracy_score(y_test, y_pred)),
        "precision_macro": float(
            precision_score(y_test, y_pred, average="macro", zero_division=0)
        ),
        "recall_macro": float(recall_score(y_test, y_pred, average="macro", zero_division=0)),
        "f1_macro": float(f1_score(y_test, y_pred, average="macro", zero_division=0)),
        "auc_ovr_macro": safe_roc_auc(y_test, y_prob),
        "confusion_matrix": confusion_matrix(y_test, y_pred, labels=CLASS_IDS).tolist(),
        "classification_report": classification_report(
            y_test,
            y_pred,
            labels=CLASS_IDS,
            target_names=[CLASS_LABELS[class_id] for class_id in CLASS_IDS],
            output_dict=True,
            zero_division=0,
        ),
    }
    return metrics


def align_probabilities(model: Any, probabilities: np.ndarray) -> np.ndarray:
    aligned = np.zeros((probabilities.shape[0], len(CLASS_IDS)), dtype=float)
    classes = model.named_steps["model"].classes_
    for source_idx, class_id in enumerate(classes):
        target_idx = CLASS_IDS.index(int(class_id))
        aligned[:, target_idx] = probabilities[:, source_idx]
    return aligned


def safe_roc_auc(y_test: pd.Series, y_prob: np.ndarray) -> float | None:
    try:
        return float(
            roc_auc_score(
                y_test,
                y_prob,
                labels=CLASS_IDS,
                multi_class="ovr",
                average="macro",
            )
        )
    except ValueError:
        return None
