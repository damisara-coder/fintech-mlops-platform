# Module Frontend React & Data Visualization — Profil 4

> **Propriétaire exclusif** : Profil 4 (Frontend React & Data Visualization Engineer)  
> **Tâches couvertes** : T7 (Affichage UMAP/SHAP) & Développement des 6 écrans de supervision

---

## 1. Objectifs & Livrables

1. **Socle Applicatif** :
   - Stack : **Vite + React 18 + TypeScript + Tailwind CSS + shadcn/ui**.
   - Client Axios avec intercepteur JWT (injection Bearer token, redirection automatique sur 401).
   - Gestion des notifications et erreurs via Toasts (zéro page blanche).
2. **Les 6 Écrans du Cahier des Charges** :
   - `Login` : Authentification JWT, sélection de l'organisation.
   - `Dashboard Principal` : 5 indicateurs KPI, Live Feed alimenté par WebSocket, distribution du risque 24h.
   - `Human Review Queue` : Écran central maître/détail, visualisations de features SHAP, boutons d'action (Approuver / Rejeter / Escalader) avec mise à jour optimiste.
   - `ML Analytics & Explicabilité` : Projection interactive UMAP (Plotly WebGL pour 8 000 points), calibration des probabilités.
   - `Simulateur de Paiement` : 3 scénarios pré-remplis (normal, fraude, incertain) pour la démo jury.
   - `System Health` : Statut des pods Kubernetes, logs de scaling HPA, état du Model Registry MLflow.
3. **Mode Autonome (MOCKS)** :
   - Utiliser [`frontend/MOCKS.md`](./MOCKS.md) pour développer l'IHM dès la Semaine 2 sans dépendance bloquante.

---

## 2. Structure Recommandée du Dossier

```text
frontend/
├── src/
│   ├── components/         # Composants shadcn/ui et widgets métier
│   ├── pages/              # Login, Dashboard, HumanReview, MLAnalytics, Simulator, Health
│   ├── services/           # api.ts (Axios), websocket.ts, mockData.ts
│   ├── hooks/              # useAuth, useWebSocket, useTransactions
│   └── types/              # Interfaces TypeScript calquées sur docs/contracts/
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── MOCKS.md
└── README.md
```
