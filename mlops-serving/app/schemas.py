from typing import Literal

from pydantic import BaseModel, ConfigDict, Field



class PredictRequest(BaseModel):
    tx_id: str = Field(min_length=1, examples=["TX-9F219"])
    amount: float = Field(ge=0.01, examples=[550.0])
    currency: str = Field(pattern=r"^[A-Z]{3}$", examples=["EUR"])
    channel: Literal["card", "wire", "sepa", "transfer"]
    ip_country: str = Field(min_length=2, max_length=2, examples=["ES"])
    ip_risk: float = Field(ge=0.0, le=1.0, examples=[0.61])
    merchant_cat: str = Field(min_length=1, examples=["electronics_new"])
    hour: int = Field(ge=0, le=23, examples=[3])
    account_age_days: int = Field(ge=0, examples=[2190])


class PredictResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    prediction: Literal[0, 1]
    probability: float = Field(ge=0.0, le=1.0)
    uncertainty_score: float = Field(ge=0.0, le=1.0)
    is_uncertain: bool
    interval: list[float] = Field(min_length=2, max_length=2)
    model_version: str
    latency_ms: int