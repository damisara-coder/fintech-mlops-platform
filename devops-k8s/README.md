# Module DevOps, Kubernetes & SRE — Profil 5

> **Propriétaire exclusif** : Profil 5 (DevOps, Kubernetes & Observability SRE)  
> **Tâches couvertes** : T8 (Docker & K8s), T9 (Tests de charge & Chaos), T10 (Monitoring & Observabilité), T11 (CI/CD DevSecOps)

---

## 1. Objectifs & Livrables

1. **Conteneurisation (T8)** :
   - Dockerfiles multi-stage pour chaque service (Spring Boot JRE slim, FastAPI Python slim, Nginx pour React).
2. **Cluster Kubernetes (T8)** :
   - Manifestes pour chaque microservice : `Deployment`, `Service` (ClusterIP), `ConfigMap`, `Secret`.
   - `Ingress` avec routage par path.
   - **Liveness & Readiness Probes** indispensables pour l'auto-guérison et le zéro-downtime.
   - **Horizontal Pod Autoscaler (HPA)** : de 4 à 12 réplicas (cible CPU : 60%).
3. **Tests de Charge k6 & Chaos Engineering (T9)** :
   - Scénarios k6 en 4 paliers : Normal ($100\text{ req/s}$) $\to$ High Load ($500$) $\to$ Overload ($2000$) $\to$ Recovery.
   - Chaos test : suppression manuelle d'un pod (`kubectl delete pod`) sous charge pour prouver la continuité de service sans perte de requête.
4. **Monitoring & Observabilité (T10)** :
   - Prometheus : scraping Actuator Spring Boot (`/actuator/prometheus`) et FastAPI (`/metrics`).
   - Grafana : 7 dashboards (Santé système, Microservices, Cluster K8s, Charge/Scalabilité, Haute disponibilité, Inférence ML, Performance modèle).
   - Règles d'alertes automatisées (CPU > 80%, latence > 2s, taux d'erreurs > 5%).
5. **Pipeline CI/CD DevSecOps (T11)** :
   - GitHub Actions : Build $\to$ Tests $\to$ Analyse SonarQube $\to$ Scan vulnérabilités Trivy $\to$ Docker Build & Push $\to$ Déploiement K8s $\to$ Smoke tests.

---

## 2. Structure Recommandée du Dossier

```text
devops-k8s/
├── docker/                 # Dockerfiles spécifiques ou partagés
├── k8s/                    # Manifestes Kubernetes (base, overlays ou helm chart)
│   ├── namespace.yaml
│   ├── configmaps/
│   ├── services/
│   ├── deployments/
│   └── ingress.yaml
├── monitoring/             # Prometheus rules, Grafana datasources & dashboards JSON
├── k6/                     # Scénarios de tests de charge (load-test.js, stress-test.js)
├── deploy.sh               # Script de déploiement en une commande
└── README.md
```
