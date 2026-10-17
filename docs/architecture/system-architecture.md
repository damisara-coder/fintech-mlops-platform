# Architecture Système & Spécifications Techniques

Ce document décrit l'architecture technique de la plateforme **FinTech MLOps**, déployée sous forme de microservices Spring Boot orchestrés avec un service de calcul Machine Learning FastAPI.

---

## 1. Cartographie des Services & Ports Réseau

| Service | Port Interne | Rôle Principal | Technologies |
|---|---|---|---|
| **api-gateway** | `8080` | Point d'entrée unique, routage, filtrage JWT, CORS, Rate Limiting | Spring Cloud Gateway |
| **auth-service** | `8081` | Gestion des utilisateurs, RBAC (4 rôles), génération et validation de tokens JWT | Spring Security, JJWT |
| **customer-service** | `8082` | Gestion des référentiels clients, comptes bancaires et historique | Spring Data JPA, PostgreSQL |
| **transaction-service** | `8083` | Réception des transactions, persistance immédiate (statut `PENDING`), appel risk | Spring Data JPA, PostgreSQL |
| **risk-service** | `8084` | Orchestrateur de risque, application de la politique §4, client FastAPI | Resilience4j, WebClient / Feign |
| **notification-service** | `8085` | Diffusion temps réel via WebSocket `/ws/tx`, alertes e-mail si blocage critique | Spring WebSocket / STOMP |
| **audit-service** | `8086` | Journalisation immuable avec hachage cryptographique SHA-256 chaîné | Spring Data JPA, PostgreSQL |
| **ml-serving (FastAPI)** | `8000` | Inférence Machine Learning, chargement artefact MLflow, conformal prediction MAPIE | FastAPI, Uvicorn, MAPIE |

---

## 2. Parcours Détaillé d'une Transaction (9 Étapes)

1. **Soumission de la transaction** : Le frontend React ou un système tiers envoie une requête `POST /api/v1/transactions` à l'**API Gateway** avec un Bearer Token JWT.
2. **Filtrage Gateway & Enregistrement** : La Gateway vérifie la validité du JWT et route la requête vers **transaction-service**. Ce dernier insère la transaction avec le statut initial `PENDING` dans PostgreSQL (traçabilité assurée même en cas de crash ultérieur).
3. **Appel interne de risque** : Le **transaction-service** interroge le **risk-service** via `POST /internal/v1/risk/score`.
4. **Appel FastAPI avec Résilience (Resilience4j)** :
   - Le **risk-service** sollicite le service ML Python via `POST /predict`.
   - **Configuration Resilience4j** :
     - *Timeout* : 800 ms.
     - *Retry* : 1 tentative.
     - *Circuit Breaker* : bascule en état ouvert après 3 échecs consécutifs.
     - *Fallback explicite* : En cas d'indisponibilité ou timeout de FastAPI, la décision bascule automatiquement en `HUMAN_REVIEW` (mode dégradé sécurisé : aucune transaction n'est perdue ni bloquée à tort).
5. **Inférence & Conformal Prediction** : **FastAPI** charge le modèle champion MLflow, calcule la probabilité de fraude et l'incertitude via Conformal Prediction (MAPIE).
6. **Application de la Règle Métier (§4)** :
   ```
   if (is_uncertain)                      --> HUMAN_REVIEW (incertitude élevée)
   else if (probability >= 0.90)          --> BLOCKED      (fraude quasi-certaine)
   else if (probability <= 0.50)          --> APPROVED     (légitime quasi-certain)
   else                                   --> HUMAN_REVIEW (zone grise)
   ```
7. **Notification Temps Réel** : Le **notification-service** diffuse l'événement sur le canal WebSocket pour actualiser le Dashboard et le Live Feed.
8. **Audit Immuable Chaîné** : L'**audit-service** calcule :
   $$\text{hash}_n = \text{SHA256}(\text{hash}_{n-1} + \text{payload}_n)$$
   et insère l'enregistrement dans la table `audit_log`.
9. **Boucle Humaine (Human-in-the-Loop)** : Si la décision est `HUMAN_REVIEW`, un analyste examine les facteurs explicatifs (SHAP) et valide ou rejette la transaction. La décision finale est tracée et alimente le jeu de ré-entraînement du modèle.

---

## 3. Observabilité Applicative

Chaque microservice Spring Boot intègre **Spring Boot Actuator** et **Micrometer Prometheus** :
- Endpoint exposé : `/actuator/prometheus`
- Métriques collectées :
  - Débit (`http_server_requests_seconds_count`)
  - Latence p95, p99 (`http_server_requests_seconds_max`)
  - Taux d'erreur HTTP 5xx
  - État de la JVM (mémoire heap, non-heap, garbage collection, threads actifs)
  - Probes Kubernetes : `/actuator/health/liveness` et `/actuator/health/readiness`
