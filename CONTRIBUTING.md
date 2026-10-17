# Guide de Contribution de l'Équipe

Bienvenue sur le projet **FinTech MLOps Platform** ! Afin de respecter le calendrier de 14 semaines sans conflit de travail, chaque membre s'engage à respecter les règles suivantes.

---

## 1. Règle Fondamentale : « Un dossier = Une personne »

- **Aucun membre ne modifie directement le dossier d'un autre profil** sans accord explicite préalable :
  - `backend/` : Profil 1
  - `ml-research/` : Profil 2
  - `mlops-serving/` : Profil 3
  - `frontend/` : Profil 4
  - `devops-k8s/` : Profil 5
- Les dossiers partagés sont `docs/contracts/` et `docs/decisions/`.

---

## 2. Règle des Contrats Gelés

- **Aucun contrat d'API ne se négocie à l'oral ou par message privé.**
- Tout format JSON, nom de champ, endpoint ou événement doit être consigné dans `docs/contracts/`.
- Toute modification d'un contrat requiert une Pull Request avec l'approbation formelle des profils impactés (ex: modification de `/predict` requiert la validation conjointe de Profil 1 et Profil 3).

---

## 3. Stratégie Git & Branches

- **Branche principale** : `main` (toujours stable, déployable).
- **Branches de travail** :
  - `feature/<profil>-<nom-de-tache>` (ex: `feature/p1-auth-jwt`, `feature/p2-eda`, `feature/p3-fastapi-predict`)
  - `fix/<profil>-<nom-du-bug>`
- **Commits conventionnels** :
  - `feat:` nouvelle fonctionnalité
  - `fix:` correction de bug
  - `test:` ajout ou modification de tests unitaires
  - `docs:` documentation ou contrats d'API
  - `refactor:` modification du code sans impact fonctionnel
  - `ci:` modifications des pipelines ou de Kubernetes

---

## 4. Processus de Pull Request

1. Travailler sur sa branche dédiée.
2. S'assurer que les tests unitaires locaux passent.
3. Ouvrir une Pull Request vers `main`.
4. Obtenir au moins 1 validation d'un pair.
5. Merger via *Squash & Merge* ou *Rebase* pour garder un historique propre.
