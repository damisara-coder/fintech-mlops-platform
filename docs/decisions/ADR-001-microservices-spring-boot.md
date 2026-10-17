# ADR-001: Adoption d'une Architecture Microservices Spring Boot & Spring Cloud

## Statut
Accepté

## Date
2026-10-17

## Contexte
La détection de fraude bancaire en temps réel requiert une haute disponibilité (99.99%), une résilience face aux pannes ponctuelles des modèles de machine learning, une traçabilité réglementaire stricte et une observabilité granulaire. Une architecture monolithique traditionnelle pose des risques de point unique de défaillance (SPOF) et rend complexe l'auto-scaling indépendant sous forte charge transactionnelle.

## Décision
1. Découper la plateforme backend en **6 microservices Spring Boot** indépendants, orchestrés par une **API Gateway Spring Cloud Gateway**.
2. Isoler le service d'inférence Machine Learning dans un conteneur dédié **FastAPI** en Python, optimisé pour les calculs vectoriels et l'inférence.
3. Implémenter le pattern **Circuit Breaker** (Resilience4j) entre le backend Java et le service d'inférence Python avec un timeout strict de 800 ms et un fallback automatique vers `HUMAN_REVIEW`.
4. Utiliser PostgreSQL avec séparation logique des schémas et indexation ciblée pour garantir l'intégrité ACID et les exigences d'auditabilité bancaire (hash chaîné SHA-256).

## Conséquences
- **Positives** :
  - Déploiement et auto-scaling Kubernetes (HPA) indépendants par service.
  - Résilience accrue : si FastAPI ou le modèle ML ralentit, le backend ne s'effondre pas et bascule en mode dégradé sécurisé.
  - Séparation stricte du travail d'équipe (règle un dossier = une personne).
- **Négatives** :
  - Complexité opérationnelle accrue (gestion de la latence réseau inter-services, observabilité distribuée requise).
