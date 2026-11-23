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

\## MLflow (T5)



Lancer le serveur (depuis mlops-serving/, venv activé) :



&#x20;   mlflow server --host 127.0.0.1 --port 5000 --backend-store-uri sqlite:///mlflow-data/mlflow.db --default-artifact-root ./mlflow-data/artifacts



UI : http://127.0.0.1:5000



Experiments : fraud-benchmark, fraud-uncertainty, fraud-champion

Registry : None -> Staging -> Production -> Archived



\### Scripts MLflow



| Script | Rôle |

|---|---|

| `python init\_experiments.py` | crée les 3 experiments et un run smoke-test |

| `python scripts/log\_benchmark.py` | enregistre le benchmark de P2 (5 runs dans `fraud-benchmark`) |

| `python scripts/log\_provisional\_model.py` | enregistre le modèle provisoire (`fraud-champion`, modèle `fraud-model`) |



\## Installation



&#x20;   pip install -r requirements.txt -r requirements-dev.txt



Versions épinglées (celles de l'entraînement de P2, voir `meta.json`) :

scikit-learn 1.9.1, pandas 2.3.3, numpy 2.4.3. Python 3.11 recommandé

(le modèle a été entraîné en 3.11.9).



\## Service FastAPI (contrat avec P1 et P5)



\- Port : 8000

\- Lancement : `uvicorn app.main:app --host 0.0.0.0 --port 8000`

\- Endpoints : `POST /predict`, `GET /health`, `GET /metrics`

\- Tests : `pytest -q` (depuis mlops-serving/)



\### Contrat de /predict (v2.0.0)



Requête :



&#x20;   {"tx\_id": "TX-9F219", "features": {"V1": -1.36, "...": 0.0, "V28": -0.02, "Amount": 550.0}}



\- 29 features obligatoires (V1..V28 + Amount, sans Time), aucun champ inconnu (sinon 422).

\- Réponse : `prediction`, `probability`, `uncertainty\_score`, `is\_uncertain`,

&#x20; `interval`, `model\_version`, `latency\_ms`.

\- FastAPI n'applique pas les seuils 0,5 / 0,9 : c'est le risk-service qui décide.

\- Les champs métier (marchand, pays...) ne sont pas envoyés au modèle : voir

&#x20; `docs/decisions/ADR-003-features-modele-vs-champs-metier.md`.



\### Choix du modèle (MODEL\_URI)



| MODEL\_URI | Comportement |

|---|---|

| vide | DummyPredictor (démo, tests) |

| `models/provisional/model.joblib` | modèle local (joblib.load puis predict\_proba) |

| `models:/fraud-model/1` ou `runs:/<run\_id>/model` | modèle chargé depuis MLflow |



\### Variables d'environnement



| Variable | Défaut | Rôle |

|---|---|---|

| `MODEL\_URI` | vide | source du modèle (voir ci-dessus) |

| `MODEL\_FALLBACK\_DUMMY` | false | repli sur le Dummy si le chargement échoue |

| `UNCERTAINTY\_THRESHOLD` | 0.6 | seuil de la règle d'incertitude provisoire |

| `SIMULATE\_DELAY\_MS` | 0 | latence artificielle (tests Resilience4j de P1) |

| `SIMULATE\_ERROR\_RATE` | 0 | taux d'erreurs 503 simulées |



\### Limites actuelles (modèle provisoire)



\- Incertitude : règle simple `1 - |2p - 1|`, pas encore de conformal (MAPIE).

\- `interval` vaut `\[p, p]` tant que le champion n'est pas livré.

\- Très peu de transactions sont envoyées en revue (\~0,05 %, cible \~1,7 %).

