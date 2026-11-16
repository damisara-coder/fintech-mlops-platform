from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class Features(BaseModel):
    """29 features brutes du modèle (schéma P2 v1.0.0). Rien d'autre n'est accepté."""
    model_config = ConfigDict(extra="forbid")

    V1: float
    V2: float
    V3: float
    V4: float
    V5: float
    V6: float
    V7: float
    V8: float
    V9: float
    V10: float
    V11: float
    V12: float
    V13: float
    V14: float
    V15: float
    V16: float
    V17: float
    V18: float
    V19: float
    V20: float
    V21: float
    V22: float
    V23: float
    V24: float
    V25: float
    V26: float
    V27: float
    V28: float
    Amount: float = Field(ge=0.0, description="Montant brut en euros")


class PredictRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    tx_id: str = Field(min_length=1, examples=["TX-9F219"])
    features: Features


class PredictResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    prediction: Literal[0, 1]
    probability: float = Field(ge=0.0, le=1.0)
    uncertainty_score: float = Field(ge=0.0, le=1.0)
    is_uncertain: bool
    interval: list[float] = Field(min_length=2, max_length=2)
    model_version: str
    latency_ms: int