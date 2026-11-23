import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression

from app.model_loader import DEFAULT_COLUMNS, SklearnPredictor, load_predictor
from app.schemas import PredictRequest


def _request():
    feats = {c: 0.1 for c in DEFAULT_COLUMNS}
    return PredictRequest(tx_id="TX-1", features=feats)


def test_without_model_uri_uses_dummy(monkeypatch):
    monkeypatch.delenv("MODEL_URI", raising=False)
    assert load_predictor().version.startswith("dummy")


def test_local_joblib_matches_predict_proba(monkeypatch, tmp_path):
    rng = np.random.default_rng(0)
    X = pd.DataFrame(rng.normal(size=(200, 29)), columns=DEFAULT_COLUMNS)
    y = (X["V14"] + rng.normal(size=200) > 0).astype(int)
    model = LogisticRegression(max_iter=500).fit(X, y)
    path = tmp_path / "model.joblib"
    joblib.dump(model, path)

    monkeypatch.setenv("MODEL_URI", str(path))
    predictor = load_predictor()
    assert isinstance(predictor, SklearnPredictor)

    out = predictor.predict(_request())
    expected = float(model.predict_proba(pd.DataFrame([[0.1] * 29], columns=DEFAULT_COLUMNS))[:, 1][0])
    assert abs(out["probability"] - expected) < 1e-6
    assert out["prediction"] == int(expected >= 0.5)
    assert out["interval"] == [out["probability"], out["probability"]]