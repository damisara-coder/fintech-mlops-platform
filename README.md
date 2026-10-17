# Plateforme FinTech MLOps — Haute Disponibilité
> **Projet JEE / LSI 2026 · Ingénierie Logicielle & MLOps**  
> Microservices Spring Boot · Machine Learning Benchmarké · Quantification d'Incertitude · FastAPI · Docker · Kubernetes · Prometheus / Grafana · React

[![CI/CD Pipeline](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-blue?logo=github-actions)](https://github.com/damisara-coder/fintech-mlops-platform/actions)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11-blue?logo=python)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110-teal?logo=fastapi)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://react.dev/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28+-blue?logo=kubernetes)](https://kubernetes.io/)

---

## 1. Vue d'Ensemble du Projet

Cette plateforme bancaire de détection de fraude est conçue pour concilier la **haute vélocité transactionnelle**, la **rigueur scientifique du Machine Learning**, et une **haute disponibilité** garantie par Kubernetes.

### 💡 L'innovation différenciante : Quantification d'Incertitude & Human-in-the-Loop
Un système classique de détection de fraude classe arbitrairement en `LÉGITIME` ou `FRAUDE`. Dans un contexte financier critique, une erreur d'un modèle trop confiant engendre soit des pertes financières massives (faux négatifs), soit l'insatisfaction de clients légitimes bloqués (faux positifs).

Notre système produit **trois décisions** :
1. **APPROUVÉE** : probabilité de fraude faible (\(\le 0.50\)) et faible incertitude.
2. **BLOQUÉE** : probabilité de fraude très élevée (\(\ge 0.90\)) et faible incertitude.
3. **HUMAN_REVIEW (Revue Humaine)** : dès que le modèle est **incertain** (Conformal Prediction via MAPIE, \(\alpha = 0.05\)) ou en cas de zone grise. La transaction est alors soumise à un analyste anti-fraude via une console interactive, et la décision humaine alimente un journal immuable qui sert de label ground-truth pour le ré-entraînement continu.

---

## 2. Architecture Globale du Système

```
┌────────────────────────────────── NAVIGATEUR (React + Vite) ──────────────────────────────────┐
│        Dashboard · Live Feed · Human Review Queue · ML Analytics · Simulator · System Health  │
└──────────────────────────────────────────────┬────────────────────────────────────────────────┘
                                               │ HTTPS / WebSocket (JWT)
┌──────────────────────────────────────────────▼────────────────────────────────────────────────┐
│                   API GATEWAY (Spring Cloud Gateway :8080) — routage, JWT, rate limit         │
└──┬──────────────┬──────────────┬─────────────────┬──────────────┬───────────────┬─────────────┘
   │              │              │                 │              │               │
   ▼              ▼              ▼                 ▼              ▼               ▼
┌───────────┐ ┌────────────┐ ┌───────────────┐ ┌────────────┐ ┌──────────────┐ ┌───────────┐
│   auth-   │ │ customer-  │ │ transaction-  │ │   risk-    │ │notification- │ │  audit-   │
│  service  │ │  service   │ │    service    │ │  service   │ │   service    │ │  service  │
│   :8081   │ │   :8082    │ │     :8083     │ │   :8084    │ │    :8085     │ │   :8086   │
└───────────┘ └──────┬─────┘ └───────┬───────┘ └─────┬──────┘ └──────┬───────┘ └─────┬─────┘
  JWT / RBAC         └───────────┬───┘               │ Resilience4j  ▲               ▼
  Spring Sec.                    ▼                   │ timeout 800ms │       ┌───────────────┐
                          ┌─────────────┐            ▼               │       │ journal SHA256│
                          │ PostgreSQL  │    ┌───────────────────────────┐   │ immuable hash │
                          │  (données)  │    │ ML SERVING (FastAPI :8000)│   │  chaîné       │
                          └─────────────┘    │ Artefact MLflow v4.2      │   └───────────────┘
                                             │ MAPIE Conformal + Metrics │
                                             └─────────────┬─────────────┘
                                                           ▼
                                             ┌───────────────────────────┐
                                             │ MLflow Registry / Server  │
                                             └───────────────────────────┘
```

---

## 3. Organisation de l'Équipe & Règle « Un dossier = Une personne »

Pour garantir l'absence de conflits de merge et une séparation nette des responsabilités, chaque membre est propriétaire exclusif de son dossier :

| Profil | Rôle | Dossier propriétaire | Périmètre technique & Tâches |
|---|---|---|---|
| **Profil 1** | **Lead Backend & Microservices** | `backend/` | 6 microservices Spring Boot, API Gateway, JPA/PostgreSQL, Resilience4j, JWT. (Tâche T1) |
| **Profil 2** | **Lead Data Scientist** | `ml-research/` | EDA, Benchmark 5 modèles, validation statistique (Friedman/Wilcoxon), incertitude MAPIE, UMAP, SHAP. (Tâches T2, T3, T4, T7-données) |
| **Profil 3** | **MLOps & Model Serving** | `mlops-serving/` | Serveur MLflow, Model Registry, microservice FastAPI, endpoint `/predict`, pipeline reproductible. (Tâches T5, T6) |
| **Profil 4** | **Frontend & Data Viz** | `frontend/` | React, TypeScript, Vite, Tailwind, shadcn/ui, WebSocket temps réel, console Human Review. (Tâches T7-UI, IHM) |
| **Profil 5** | **DevOps, Kubernetes & SRE** | `devops-k8s/` | Déploiements K8s, HPA, Probes liveness/readiness, Prometheus, Grafana, tests de charge k6, CI/CD. (Tâches T8, T9, T10, T11) |

---

## 4. Les 11 Tâches du Cahier des Charges

- **T1 — Architecture Microservices FinTech** : 6 microservices Spring Boot + API Gateway, communication interne sécurisée, JWT.
- **T2 — Classification ML & Benchmark** : Comparaison de 5 algorithmes (Logistic Regression, Random Forest, XGBoost, SVM, MLP).
- **T3 — Validation Statistique** : Tests de Friedman, Wilcoxon signé, correction de Holm, intervalles de confiance à 95 %.
- **T4 — Quantification d'Incertitude** : Conformal Prediction (MAPIE), calibration des probabilités, Human-in-the-Loop.
- **T5 — MLflow & MLOps** : Tracking des expériences, Model Registry (Staging/Production), reproductibilité.
- **T6 — Microservice FastAPI Inférence** : `POST /predict`, validation Pydantic, métriques `/metrics`, temps d'inférence < 50ms.
- **T7 — Visualisation de Données & Explicabilité** : Projections 2D UMAP/PCA, contributions locales SHAP.
- **T8 — Conteneurisation & Déploiement Kubernetes** : Multi-stage Dockerfiles, Services, Ingress, RollingUpdate, HPA (min 4, max 12 réplicas).
- **T9 — Tests de Charge & Haute Disponibilité** : Scénarios k6 (100 req/s → 2000 req/s), Chaos engineering (`kubectl delete pod`).
- **T10 — Observabilité & Monitoring** : Prometheus (Actuator + FastAPI), 7 dashboards Grafana, alertes automatisées.
- **T11 — CI/CD & DevSecOps** : GitHub Actions, analyse SonarQube, scan de vulnérabilités Trivy, déploiement automatisé.

---

## 5. Contrats d'API Gelés (Zéro Conflit d'Intégration)

Conformément à la règle d'intégration, aucun contrat ne se négocie à l'oral. Tous les contrats formels sont stockés dans le dossier [`docs/contracts/`](file:///docs/contracts/) :
- **`/predict` (FastAPI)** : [`docs/contracts/predict-contract.json`](file:///docs/contracts/predict-contract.json)
- **`/internal/v1/risk/score` (Backend Spring)** : [`docs/contracts/risk-score-contract.json`](file:///docs/contracts/risk-score-contract.json)
- **Événements WebSocket** : [`docs/contracts/websocket-events.json`](file:///docs/contracts/websocket-events.json)
- **Spécification OpenAPI globale** : [`docs/contracts/openapi-spec.yaml`](file:///docs/contracts/openapi-spec.yaml)
- **Jeux de données simulées pour le Frontend** : [`frontend/MOCKS.md`](file:///frontend/MOCKS.md)

---

## 6. Structure du Dépôt

```text
fintech-mlops-platform/
├── .github/workflows/             # Pipelines CI/CD DevSecOps
├── backend/                       # Modules Maven Spring Boot (Lead Backend)
│   ├── pom.xml                    # POM parent multi-module
│   ├── api-gateway/               # Spring Cloud Gateway (:8080)
│   ├── auth-service/              # Authentification & JWT (:8081)
│   ├── customer-service/          # Gestion clients et comptes (:8082)
│   ├── transaction-service/       # Ingestion transactions (:8083)
│   ├── risk-service/              # Orchestration & décision (§4) (:8084)
│   ├── notification-service/      # WebSocket temps réel (:8085)
│   └── audit-service/             # Journal immuable SHA-256 (:8086)
├── ml-research/                   # Notebooks, benchmarks, validation statistique
├── mlops-serving/                 # API FastAPI, intégration modèle champion MLflow
├── frontend/                      # IHM React/Vite, Dashboard, Human Review Queue
├── devops-k8s/                    # Manifestes Kubernetes, Helm, k6, Grafana dashboards
└── docs/                          # Architecture, ADRs, schémas SQL et contrats d'API
```

---

## 7. Démarrage Rapide

### Prérequis
- Java 17+ & Maven 3.8+
- Python 3.10+
- Node.js 18+
- Docker & Docker Compose / Minikube (ou Kind)

### Démarrage avec Docker Compose (Environnement Local)
```bash
# Cloner le projet
git clone https://github.com/damisara-coder/fintech-mlops-platform.git
cd fintech-mlops-platform

# Déployer l'environnement complet
chmod +x devops-k8s/deploy.sh
./devops-k8s/deploy.sh
```

---

## 8. Livrables & Démonstration

1. **Dépôt Git** : Code source complet, historique de commits propre, tests unitaires et d'intégration.
2. **README Exhaustif** : Résultats scientifiques chiffrés, preuves de tests de charge k6 et observabilité.
3. **Vidéo de Démonstration (≤ 5 min)** : Parcours complet d'une transaction, intervention humaine, bascule de modèle et auto-guérison Kubernetes.
