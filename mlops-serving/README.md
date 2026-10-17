# Module MLOps & Model Serving — Profil 3

> **Propriétaire exclusif** : Profil 3 (MLOps & FastAPI Model Serving Engineer)  
> **Tâches couvertes** : T5 (MLflow & Tracking), T6 (Microservice FastAPI `/predict`)

---

## 1. Objectifs & Livrables

1. **Serveur & Registry MLflow (T5)** :
   - Tracking complet des runs, hyperparamètres, métriques et artefacts.
   - Model Registry avec promotion de stades : `Staging` -> `Production`.
2. **Microservice FastAPI (T6)** :
   - Point d'entrée : `POST /predict` strictement conforme au contrat : [`docs/contracts/predict-contract.json`](../docs/contracts/predict-contract.json).
   - Chargement unique du modèle en mémoire au démarrage (`lifespan`).
   - Validation stricte des entrées via modèles **Pydantic**.
   - Exposition des endpoints de monitoring : `GET /health` et `GET /metrics` (Prometheus).
   - Latence d'inférence cible : $< 50$ ms.
3. **Pipeline Reproductible** :
   - Ingestion -> Entraînement -> Évaluation -> Test statistique -> Registry -> Déploiement.

---

## 2. Structure Recommandée du Dossier

```text
mlops-serving/
├── app/
│   ├── main.py             # Application FastAPI, routes /predict, /health, /metrics
│   ├── schemas.py          # Modèles Pydantic Request/Response
│   ├── model_loader.py     # Chargement MLflow & cache
│   └── predictor.py        # Logique d'inférence et calcul d'incertitude
├── tests/                  # Tests unitaires PyTest (cas nominaux, erreurs, timeouts)
├── Dockerfile              # Image Python multi-stage optimisée (slim)
├── requirements.txt        # fastapi, uvicorn, pydantic, mlflow, prometheus-client
└── README.md
```
