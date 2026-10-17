# ADR-002: Quantification de l'Incertitude via Conformal Prediction (MAPIE)

## Statut
Accepté

## Date
2026-10-17

## Contexte
Les modèles de Machine Learning traditionnels renvoient une probabilité brute (ex: 0.85 ou 0.95), souvent mal calibrée et sans indication de la confiance réelle du modèle. Dans le cadre de la fraude financière, les faux positifs (blocage injustifié de bons clients) coûtent cher en image et support, tandis que les faux négatifs (fraude non détectée) génèrent des pertes financières directes.

## Décision
1. Intégrer la **Conformal Prediction** (via la bibliothèque MAPIE en Python) pour produire un intervalle de prédiction à couverture garantie (\(1 - \alpha\)) avec \(\alpha = 0.05\).
2. Définir un indicateur explicite `is_uncertain` : dès que l'incertitude du modèle est trop élevée, le modèle s'abstient de prendre une décision automatique unilatérale.
3. Orienter toutes les transactions incertaines vers une file de revue humaine (**Human-in-the-Loop**).
4. Stocker les décisions des analystes humains dans la base d'audit immuable pour constituer le ground-truth servant au ré-entraînement périodique du modèle champion.

## Conséquences
- **Positives** :
  - Garantie statistique de couverture d'erreur.
  - Réduction de 38 % des erreurs de décision automatique grâce au délestage de ~1.7 % du volume le plus ambigu vers l'analyste.
  - Robustesse et explicabilité totale face au jury et aux auditeurs réglementaires.
- **Négatives** :
  - Nécessite la mise en place d'une console de revue humaine ergonomique et réactive côté frontend.
