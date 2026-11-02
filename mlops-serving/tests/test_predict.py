from fastapi.testclient import TestClient

from app.main import app

VALID = {
    "tx_id": "TX-9F219", "amount": 550.0, "currency": "EUR", "channel": "card",
    "ip_country": "ES", "ip_risk": 0.61, "merchant_cat": "electronics_new",
    "hour": 3, "account_age_days": 2190,
}


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
        assert c.post("/predict", json={**VALID, "hour": 24}).status_code == 422
        assert c.post("/predict", json={**VALID, "amount": 0}).status_code == 422
        assert c.post("/predict", json={**VALID, "channel": "cash"}).status_code == 422
        bad = {k: v for k, v in VALID.items() if k != "ip_risk"}
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