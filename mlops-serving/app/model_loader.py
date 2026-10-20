import os

from .predictor import DummyPredictor


def load_predictor():
    """Appelé UNE SEULE FOIS au démarrage. MODEL_URI permet de swapper
    provisoire / champion sans toucher au code."""
    model_uri = os.getenv("MODEL_URI", "dummy")
    if model_uri == "dummy":
        return DummyPredictor()
    # TODO P3.2 / P3.5 : mlflow.pyfunc.load_model(model_uri) + MAPIE
    raise NotImplementedError(f"MODEL_URI non supporté pour l'instant : {model_uri}")