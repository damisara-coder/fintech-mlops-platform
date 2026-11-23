\# ADR-003: Features du modèle vs champs métier



\## Statut

Proposé (à valider par P1 et P2)



\## Date

2026-11-23



\## Contexte

Le modèle de P2 est entraîné sur le dataset ULB : V1..V28 sont des composantes PCA anonymisées, plus Amount. La Maquette 7 du cadrage listait des champs métier (merchant\_cat, ip\_country, account\_age\_days...) pour POST /predict, ce qui date d'avant le choix du dataset.



\## Décision

1\. FastAPI POST /predict reçoit `{tx\_id, features}` avec 29 colonnes (V1..V28 + Amount), conformément à `predict-contract.json` v2.0.0.

2\. Aucun mapping métier vers V1..V28 dans FastAPI : la transformation PCA d'origine est inconnue, donc non inversible.

3\. Les champs métier (marchand, pays, canal, ancienneté du compte) sont stockés en base et affichés dans l'UI, mais ne sont pas envoyés au modèle.

4\. Le simulateur associe à chaque scénario (normal, fraude, incertain) un vecteur `features` tiré des cas de test de P2, plus des champs métier cohérents pour l'affichage.



\## Conséquences

\- \*\*Positives\*\* :

&#x20; - Contrat unique, vérifiable, sans transformation opaque entre le backend et le modèle.

&#x20; - Le service FastAPI reste indépendant de l'algorithme (joblib.load puis predict\_proba).

\- \*\*Négatives\*\* :

&#x20; - Le panneau d'explication (SHAP) affichera V14, V4... : libellés génériques côté front.

&#x20; - Une vraie ingénierie de features métier est une évolution future, à citer dans le README.

\- \*\*Actions\*\* :

&#x20; - P1 : dans `predict-contract.json`, `required` = les 29 champs et `additionalProperties` = false.

&#x20; - P2 : tant que le champion n'est pas livré, `interval` vaut `\[p, p]` et l'incertitude est le placeholder `1 - abs(2p - 1)`.

