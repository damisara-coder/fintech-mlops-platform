"""Chargement unique du modele (P3). Independant de l'algorithme :
joblib.load (ou MLflow) puis predict_proba, comme demande par P2.

MODEL_URI :
  - vide                         -> DummyPredictor (repli)
  - chemin vers un .joblib       -> modele local (ex. models/provisional/model.joblib)
  - models:/fraud-model/1 ou     -> modele charge depuis MLflow
    runs:/<run_id>/model
"""
import json
import logging
import os
from pathlib import Path

import pandas as pd

from .predictor import DummyPredictor
from .schemas import PredictRequest

log = logging.getLogger("model_loader")

DEFAULT_COLUMNS = [f"V{i}" for i in range(1, 29)] + ["Amount"]
UNCERTAINTY_THRESHOLD = float(os.getenv("UNCERTAINTY_THRESHOLD", "0.6"))


class SklearnPredictor:
    """Enveloppe un Pipeline sklearn (ou tout objet avec predict_proba)."""

    def __init__(self, model, version: str, columns: list[str]):
        self.model = model
        self.version = version
        self.columns = columns

    def predict(self, req: PredictRequest) -> dict:
        feats = req.features.model_dump()
        # DataFrame avec les noms exacts, dans l'ordre du modele (exige par P2)
        df = pd.DataFrame([[feats[c] for c in self.columns]], columns=self.columns)
        p = float(self.model.predict_proba(df)[:, 1][0])  # non arrondi

        # Regle provisoire (placeholder P2) : remplacee par MAPIE avec le champion
        uncertainty = 1 - abs(2 * p - 1)
        return {
            "prediction": int(p >= 0.5),
            "probability": p,
            "uncertainty_score": uncertainty,
            "is_uncertain": uncertainty >= UNCERTAINTY_THRESHOLD,
            "interval": [p, p],  # substitut tant qu'il n'y a pas de conformal
            "model_version": self.version,
        }


def _read_meta(folder: Path) -> dict:
    meta_file = folder / "meta.json"
    if meta_file.exists():
        return json.loads(meta_file.read_text(encoding="utf-8"))
    return {}


def _load_local(path: Path) -> SklearnPredictor:
    import joblib

    model = joblib.load(path)
    meta = _read_meta(path.parent)
    columns = meta.get("input_columns", DEFAULT_COLUMNS)
    version = meta.get("model_version", path.stem)
    return SklearnPredictor(model, version, columns)


def _load_mlflow(uri: str) -> SklearnPredictor:
    import mlflow.sklearn

    model = mlflow.sklearn.load_model(uri)
    version = os.getenv("MODEL_VERSION", uri)
    return SklearnPredictor(model, version, DEFAULT_COLUMNS)


def load_predictor():
    uri = os.getenv("MODEL_URI", "").strip()
    if not uri:
        log.warning("MODEL_URI vide : DummyPredictor utilise")
        return DummyPredictor()
    try:
        if uri.startswith(("models:/", "runs:/")):
            return _load_mlflow(uri)
        return _load_local(Path(uri))
    except Exception:
        log.exception("Echec du chargement de %s", uri)
        if os.getenv("MODEL_FALLBACK_DUMMY", "false").lower() == "true":
            return DummyPredictor()
        raise