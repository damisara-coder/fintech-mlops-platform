#!/usr/bin/env bash
# =========================================================================
# Environnement local reproductible (Plateforme FinTech MLOps) - Profil 5
# Usage : bash devops-k8s/deploy.sh
# Idempotent : relancer le script ne recree rien d'existant.
# =========================================================================
set -euo pipefail

CLUSTER_NAME="fintech"
NAMESPACE="fintech"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fail() { echo "ERREUR : $1" >&2; exit 1; }

echo "=== [FinTech MLOps] Environnement local ==="

echo "1/4. Verification des prerequis..."
for cmd in docker kubectl kind; do
    command -v "$cmd" >/dev/null 2>&1 || fail "$cmd n'est pas installe."
done
docker info >/dev/null 2>&1 || fail "Docker ne repond pas (Docker Desktop est-il demarre ?)."

echo "2/4. PostgreSQL (docker compose)..."
cd "$ROOT_DIR"
docker compose up -d postgres
for i in $(seq 1 30); do
    status="$(docker inspect -f '{{.State.Health.Status}}' fintech-postgres 2>/dev/null || echo absent)"
    [ "$status" = "healthy" ] && break
    sleep 2
done
[ "$status" = "healthy" ] || fail "PostgreSQL n'est pas 'healthy' (etat : $status)."
echo "     PostgreSQL : healthy"

echo "3/4. Cluster Kubernetes (kind '$CLUSTER_NAME')..."
if kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
    echo "     Le cluster existe deja, il est reutilise."
else
    kind create cluster --name "$CLUSTER_NAME"
fi
kubectl config use-context "kind-$CLUSTER_NAME" >/dev/null
kubectl wait --for=condition=Ready node --all --timeout=120s

echo "4/4. Namespace '$NAMESPACE'..."
kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || kubectl create namespace "$NAMESPACE"

echo "=== Environnement pret ==="
echo "Base de donnees : conteneur fintech-postgres (port selon .env, 5432 par defaut)"
echo "Cluster         : kind-$CLUSTER_NAME, namespace $NAMESPACE"
echo "A venir         : images Docker (P5.4) et manifests Kubernetes (P5.5)."