# Backend Microservices — Profil 1 (Lead Backend)

> **Propriétaire exclusif** : Profil 1 (Lead Backend & Microservices Spring Boot)  
> **Tâches couvertes** : T1 (Architecture Microservices FinTech)  
> **Stack** : Java 17, Spring Boot 3.3+, Spring Cloud 2023, Spring Security (JWT), Spring Data JPA, PostgreSQL, Resilience4j

---

## 1. Modules & Ports Réseau

| Module | Port | Rôle |
|---|---|---|
| `api-gateway` | `8080` | Spring Cloud Gateway, routage central, validation JWT, CORS |
| `auth-service` | `8081` | Gestion utilisateurs, RBAC (ANALYST, RISK_OPS, DEV, ADMIN), JWT |
| `customer-service` | `8082` | Gestion des clients, comptes et historique bancaire |
| `transaction-service`| `8083` | Enregistrement des transactions (statut PENDING) et appel risk-service |
| `risk-service` | `8084` | Orchestrateur de risque, règle §4, client FastAPI avec Resilience4j |
| `notification-service`| `8085` | WebSocket temps réel (`/ws/tx`) et alertes |
| `audit-service` | `8086` | Journal immuable à hash chaîné SHA-256 (auditabilité réglementaire) |

---

## 2. Compilation & Construction

Depuis la racine du dossier `backend/` :
```bash
# Compiler l'ensemble des modules
mvn clean package -DskipTests

# Lancer les tests unitaires avec couverture SonarQube (> 70%)
mvn clean test
```

---

## 3. Configuration Locale & Profils

Chaque microservice s'exécute avec les profils Spring configurables :
- `SPRING_DATASOURCE_URL` : URL de la base PostgreSQL (défaut : `jdbc:postgresql://localhost:5432/fintech_db`)
- `ML_SERVICE_URL` : URL de l'API FastAPI (défaut : `http://localhost:8000`)
- Les endpoints Actuator `/actuator/prometheus` et `/actuator/health` sont exposés sur chaque service pour l'observabilité SRE (Profil 5).
