import math

from .schemas import PredictRequest


class DummyPredictor:
    """Modèle factice déterministe pour débloquer P1 et P4.
    Même interface que le futur modèle MLflow : predict(req) -> dict."""

    version = "dummy-0.1"
    UNCERTAINTY_THRESHOLD = 0.30

    def predict(self, req: PredictRequest) -> dict:
        z = -3.0
        z += 2.5 * req.ip_risk
        z += 1.5 if req.amount > 5000 else (0.6 if req.amount > 500 else 0.0)
        z += 0.8 if req.hour < 6 else 0.0
        z += 0.7 if req.account_age_days < 30 else -0.4
        z += 0.6 if req.merchant_cat.endswith("_new") else 0.0
        p = 1 / (1 + math.exp(-z))

        uncertainty = 1 - abs(2 * p - 1)  # max quand p ~ 0.5
        half = uncertainty / 4
        return {
            "prediction": int(p >= 0.5),
            "probability": round(p, 4),
            "uncertainty_score": round(uncertainty, 4),
            "is_uncertain": uncertainty > self.UNCERTAINTY_THRESHOLD,
            "interval": [round(max(0.0, p - half), 4), round(min(1.0, p + half), 4)],
            "model_version": self.version,
        }