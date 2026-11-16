from fastapi.testclient import TestClient

from app.main import app

FEATURES = {f"V{i}": 0.0 for i in range(1, 29)} | {"Amount": 12.4}
VALID = {"tx_id": "TX-9F219", "features": FEATURES}


def test_predict_matches_contract():
    with TestClient(app) as c:
        r = c.post("/predict", json=VALID)
    assert r.status_code == 200
    body = r.json()
    assert set(body) == {"prediction", "probability", "uncertainty_score",
                         "is_uncertain", "interval", "model_version", "latency_ms"}
    assert body["prediction"] in (0, 1)
    assert len(body["interval"]) == 2
    assert isinstance(body["latency_ms"], int)


def test_invalid_inputs_are_rejected():
    with TestClient(app) as c:
        # Amount négatif
        neg = {**VALID, "features": {**FEATURES, "Amount": -1}}
        assert c.post("/predict", json=neg).status_code == 422
        # champ manquant
        missing = {k: v for k, v in FEATURES.items() if k != "V14"}
        assert c.post("/predict", json={**VALID, "features": missing}).status_code == 422
        # champ en trop (ancien schéma ou Time)
        extra = {**VALID, "features": {**FEATURES, "Time": 100.0}}
        assert c.post("/predict", json=extra).status_code == 422
        # tx_id manquant
        assert c.post("/predict", json={"features": FEATURES}).status_code == 422
        # type invalide
        bad = {**VALID, "features": {**FEATURES, "V1": "abc"}}
        assert c.post("/predict", json=bad).status_code == 422



def test_health_and_metrics():
    with TestClient(app) as c:
        assert c.get("/health").json()["model_loaded"] is True
        assert "predict_requests_total" in c.get("/metrics").text


def test_health_returns_200_when_model_loaded():
    with TestClient(app) as c:
        r = c.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"
    assert r.json()["model_loaded"] is True


def test_health_returns_503_when_model_missing():
    with TestClient(app) as c:
        original = app.state.predictor
        app.state.predictor = None
        try:
            r = c.get("/health")
        finally:
            app.state.predictor = original
    assert r.status_code == 503
    body = r.json()
    assert body["status"] == "unavailable"
    assert body["model_loaded"] is False
    assert body["model_version"] is None