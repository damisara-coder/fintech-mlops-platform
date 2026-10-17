# Jeux de Données Simulées (MOCKS.md) — Frontend React

> **Règle Anti-Conflit C2** : Ce fichier permet au **Profil 4 (Frontend)** de développer l'intégralité des 6 écrans de l'interface utilisateur dès la Semaine 2, en toute indépendance, sans attendre que le backend ou le modèle ML soient finalisés.

---

## 1. Connexion / Auth (`POST /api/v1/auth/login`)

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbWluYUBiYW5rLWZpbnRlY2guY29tIiwicm9sZSI6IkFOQUxZU1QiLCJleHAiOjE3NjA3MzYwMDB9.signature",
  "user": {
    "id": "USR-01",
    "email": "amina@bank-fintech.com",
    "name": "Amina",
    "role": "ANALYST",
    "organization": "Risk Operations"
  },
  "expires_in": 3600
}
```

---

## 2. Indicateurs du Dashboard (`GET /api/v1/dashboard/kpis`)

```json
{
  "transactions_24h": 184392,
  "transactions_trend": "+4.2% vs hier",
  "blocked_fraud": 1204,
  "blocked_ratio": "0.65% du volume",
  "human_review_queue": 318,
  "human_review_trend": "+41 en 1h",
  "ml_latency_p95": 42,
  "ml_latency_target": 50,
  "pods_running": "14/14",
  "hpa_status": "min 4 -> max 12 (actif)"
}
```

---

## 3. Human Review Queue (`GET /api/v1/transactions/human-review`)

```json
[
  {
    "tx_id": "TX-9F219",
    "client": {
      "id": "CUST-10428",
      "name": "L. Benali",
      "account_age_days": 2190,
      "total_transactions": 214,
      "fraud_history": 0,
      "confidence_score": 0.99
    },
    "transaction": {
      "amount": 550.00,
      "currency": "EUR",
      "channel": "Card : ES (new)",
      "merchant_category": "electronics_new",
      "country": "ES",
      "hour": 3,
      "is_recurring_merchant": false,
      "three_d_secure_passed": true,
      "created_at": "2026-12-05T03:12:44Z"
    },
    "model_evaluation": {
      "decision": "HUMAN_REVIEW",
      "predicted_class": "FRAUDE",
      "probability": 0.52,
      "uncertainty_score": 0.34,
      "conformal_interval": [0.48, 0.56],
      "model_version": "v4.2"
    },
    "shap_factors": [
      { "feature": "merchant_category = electronics_new", "contribution": 0.31, "type": "fraud_risk" },
      { "feature": "ip_country_risk = 0.61", "contribution": 0.22, "type": "fraud_risk" },
      { "feature": "hour_deviation = +3.1 h", "contribution": 0.14, "type": "fraud_risk" },
      { "feature": "tx_count_24h = 2", "contribution": -0.12, "type": "safe" },
      { "feature": "account_age_days = 2190", "contribution": -0.18, "type": "safe" },
      { "feature": "is_recurring_merchant = 0", "contribution": -0.05, "type": "safe" },
      { "feature": "3d_secure_passed = 1", "contribution": 0.00, "type": "neutral" }
    ],
    "queue_sla_remaining_minutes": 4
  }
]
```

---

## 4. Scénarios du Simulateur de Paiement (`POST /api/v1/transactions/simulate`)

### Scénario 1 : Transaction Normale
- **Requête** : Achat normal, 12.40 €, Paris FR, Boutique en ligne habituelle
- **Réponse simulée** :
```json
{
  "tx_id": "TX-NORM-01",
  "status": "APPROVED",
  "probability": 0.04,
  "uncertainty_score": 0.01,
  "is_uncertain": false,
  "latency_ms": 28,
  "model_version": "v4.2",
  "audit_id": "a-99120"
}
```

### Scénario 2 : Fraude Évidente
- **Requête** : Virement 8 000 €, IP Russe, Nouveau compte
- **Réponse simulée** :
```json
{
  "tx_id": "TX-FRAUD-02",
  "status": "BLOCKED",
  "probability": 0.97,
  "uncertainty_score": 0.02,
  "is_uncertain": false,
  "latency_ms": 35,
  "model_version": "v4.2",
  "audit_id": "a-99121",
  "alert": "Alerte critique émise vers notification-service"
}
```

### Scénario 3 : Transaction Ambiguë / Incertaine
- **Requête** : Achat 550 €, Nouveau marchand, 3h du matin en Espagne
- **Réponse simulée** :
```json
{
  "tx_id": "TX-9F219",
  "status": "HUMAN_REVIEW",
  "probability": 0.52,
  "uncertainty_score": 0.34,
  "is_uncertain": true,
  "conformal_interval": [0.48, 0.56],
  "latency_ms": 38,
  "model_version": "v4.2",
  "audit_id": "a-88213"
}
```
