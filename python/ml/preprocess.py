from __future__ import annotations

from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder
from sklearn.tree import DecisionTreeClassifier

try:
    from .constants import (
        NUMERIC_FEATURES,
        ONE_HOT_FEATURES,
        ORDINAL_FEATURES,
        RANDOM_STATE,
    )
except ImportError:
    from constants import (
        NUMERIC_FEATURES,
        ONE_HOT_FEATURES,
        ORDINAL_FEATURES,
        RANDOM_STATE,
    )


def build_preprocessor() -> ColumnTransformer:
    return ColumnTransformer(
        transformers=[
            ("num", SimpleImputer(strategy="median"), NUMERIC_FEATURES),
            ("ord", SimpleImputer(strategy="most_frequent"), ORDINAL_FEATURES),
            (
                "ethnicity",
                Pipeline(
                    steps=[
                        ("imputer", SimpleImputer(strategy="most_frequent")),
                        ("onehot", OneHotEncoder(handle_unknown="ignore")),
                    ]
                ),
                ONE_HOT_FEATURES,
            ),
        ],
        remainder="drop",
    )


def build_pipeline() -> Pipeline:
    return Pipeline(
        steps=[
            ("preprocessor", build_preprocessor()),
            ("model", DecisionTreeClassifier(random_state=RANDOM_STATE)),
        ]
    )


def default_param_grid() -> dict[str, list[object]]:
    return {
        "model__criterion": ["gini", "entropy"],
        "model__max_depth": [3, 4, 5, 6, 8, 10],
        "model__min_samples_split": [2, 5, 10, 20],
        "model__min_samples_leaf": [1, 2, 5, 10],
        "model__max_leaf_nodes": [None, 20, 30],
        "model__ccp_alpha": [0.0, 0.0005, 0.001],
    }


def exhaustive_param_grid() -> dict[str, list[object]]:
    return {
        "model__criterion": ["gini", "entropy", "log_loss"],
        "model__max_depth": [3, 4, 5, 6, 8, 10, None],
        "model__min_samples_split": [2, 5, 10, 20],
        "model__min_samples_leaf": [1, 2, 5, 10],
        "model__max_leaf_nodes": [None, 10, 20, 30, 50],
        "model__ccp_alpha": [0.0, 0.0005, 0.001, 0.005, 0.01],
    }


def smoke_param_grid() -> dict[str, list[object]]:
    """The former default grid: fast enough for frequent local retraining."""
    return {
        "model__criterion": ["gini", "entropy"],
        "model__max_depth": [4, 6, 8],
        "model__min_samples_split": [2, 10],
        "model__min_samples_leaf": [1, 5],
        "model__max_leaf_nodes": [None, 20],
        "model__ccp_alpha": [0.0, 0.001],
    }
