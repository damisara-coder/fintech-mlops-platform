# Module ML Research — Profil 2 (Lead Data Scientist)

> **Propriétaire exclusif** : Profil 2 (Lead Data Scientist)  
> **Tâches couvertes** : T2 (Benchmark), T3 (Validation statistique), T4 (Incertitude MAPIE), T7 (Données UMAP/PCA & SHAP)

---

## 1. Objectifs & Livrables

1. **Jeu de données (`datasets/`)** :
   - Dataset de fraude (Credit Card Fraud ou IEEE-CIS).
   - Gestion du déséquilibre de classes (SMOTE ou `class_weight`, jamais d'undersampling naïf).
2. **Benchmark 5 Algorithmes (T2)** :
   - Comparaison sur métriques adaptées aux classes rares : **PR-AUC, MCC, ROC-AUC, F1-Score, Brier Score, Latence**.
   - Modèles : Logistic Regression, Random Forest, XGBoost, SVM (RBF), MLP.
3. **Validation Statistique Rigoureuse (T3)** :
   - Validation croisée répétée ($10 \times 5$ folds).
   - Test global de **Friedman** ($p < 0.05$).
   - Tests post-hoc de **Wilcoxon signé** par paires avec correction de **Holm**.
   - Tailles d'effet (Cohen's d) et Intervalles de Confiance à 95% bootstrappés.
4. **Quantification d'Incertitude (T4)** :
   - Conformal Prediction via **MAPIE**.
   - Calibration des probabilités (Platt Scaling ou Régression Isotonique).
   - Détermination du seuil $\alpha$ (cible : ~1.7% du volume en revue humaine).
5. **Artefacts pour l'intégration (T7 & P3)** :
   - Export du modèle champion et du pipeline de feature engineering vers `mlops-serving/`.
   - Export des projections 2D/3D (UMAP/PCA) et des valeurs SHAP au format JSON pour `frontend/`.

---

## 2. Structure Recommandée du Dossier

```text
ml-research/
├── datasets/             # Données (fichiers légers / sample.csv - voir .gitignore)
├── notebooks/            # 01_eda.ipynb, 02_benchmark.ipynb, 03_conformal.ipynb
├── src/                  # Code python réutilisable (features, training, evaluation)
├── requirements.txt      # Dépendances épinglées (scikit-learn, xgboost, mapie, etc.)
└── README.md
```
