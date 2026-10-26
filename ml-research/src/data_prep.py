"""Préparation des données (P2.2) - module partagé par tous les notebooks de ml-research.

Règle d'or : TOUT ce qui est appris sur les données (moyennes, écarts-types, SMOTE...)
est appris sur le lot d'ENTRAÎNEMENT uniquement. Calibration et test ne servent qu'à transformer/évaluer.
"""
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import FunctionTransformer, StandardScaler

SEED = 42
TARGET = "Class"
V_COLS = [f"V{i}" for i in range(1, 29)]


def load_clean(path):
    """Charge le CSV et supprime les doublons (avant tout découpage)."""
    df = pd.read_csv(path)
    return df.drop_duplicates().reset_index(drop=True)


def make_splits(df, seed=SEED):
    """Découpage stratifié : 60 % entraînement / 20 % calibration / 20 % test."""
    X, y = df.drop(columns=TARGET), df[TARGET]
    X_tmp, X_test, y_tmp, y_test = train_test_split(
        X, y, test_size=0.20, stratify=y, random_state=seed
    )
    # 25 % des 80 % restants = 20 % du total
    X_train, X_cal, y_train, y_cal = train_test_split(
        X_tmp, y_tmp, test_size=0.25, stratify=y_tmp, random_state=seed
    )
    return {"train": (X_train, y_train), "cal": (X_cal, y_cal), "test": (X_test, y_test)}


def build_preprocessor(include_time=False):
    """log(1 + Amount) puis standardisation ; V1..V28 standardisées ; Time optionnel."""
    transformers = [
        ("amount", Pipeline([("log", FunctionTransformer(np.log1p)),
                             ("scale", StandardScaler())]), ["Amount"]),
        ("v", StandardScaler(), V_COLS),
    ]
    if include_time:
        transformers.append(("time", StandardScaler(), ["Time"]))
    return ColumnTransformer(transformers, remainder="drop")


def input_columns(include_time=False):
    """Colonnes BRUTES attendues par le pipeline (ce que P3 devra envoyer)."""
    return V_COLS + ["Amount"] + (["Time"] if include_time else [])
