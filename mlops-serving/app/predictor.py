import math

from .schemas import PredictRequest


class DummyPredictor:
    """Modèle factice déterministe pour débloquer P1 et P4.
    Même interface que le futur modèle MLflow : predict(req) -> dict."""

    version = "dummy-0.2"
    UNCERTAINTY_THRESHOLD = 0.30

    def predict(self, req: PredictRequest) -> dict:
        f = req.features
        z = -4.0
        z += -0.5 * f.V14          # V14 très négatif => fraude
        z += -0.3 * f.V17
        z += 1.0 if f.Amount > 1000 else 0.0
        z = max(-30.0, min(30.0, z))
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