import asyncio
import os
import random
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import JSONResponse
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest

from .model_loader import load_predictor
from .schemas import PredictRequest, PredictResponse

REQUESTS = Counter("predict_requests_total", "Requêtes /predict", ["status"])
UNCERTAIN = Counter("predict_uncertain_total", "Prédictions is_uncertain=true")
LATENCY = Histogram(
    "predict_latency_seconds", "Latence d'inférence",
    buckets=(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0),
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.predictor = load_predictor()  # chargement unique
    yield


app = FastAPI(title="Fraud ML Serving", version="1.0.0", lifespan=lifespan)


@app.get("/health")
def health(request: Request):
    p = getattr(request.app.state, "predictor", None)
    loaded = p is not None
    body = {
        "status": "ok" if loaded else "unavailable",
        "model_loaded": loaded,
        "model_version": p.version if loaded else None,
    }
    return JSONResponse(status_code=200 if loaded else 503, content=body)



@app.get("/metrics")
def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.post("/predict", response_model=PredictResponse)
async def predict(req: PredictRequest, request: Request):
    # Simulation de pannes pour que P1 teste Resilience4j (désactivée par défaut)
    delay = int(os.getenv("SIMULATE_DELAY_MS", "0"))
    if delay:
        await asyncio.sleep(delay / 1000)
    if random.random() < float(os.getenv("SIMULATE_ERROR_RATE", "0")):
        REQUESTS.labels("error").inc()
        raise HTTPException(status_code=503, detail="simulated failure")

    start = time.perf_counter()
    result = request.app.state.predictor.predict(req)
    elapsed = time.perf_counter() - start

    LATENCY.observe(elapsed)
    REQUESTS.labels("ok").inc()
    if result["is_uncertain"]:
        UNCERTAIN.inc()
    return PredictResponse(**result, latency_ms=int(elapsed * 1000))