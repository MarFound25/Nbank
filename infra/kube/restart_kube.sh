#!/usr/bin/env bash
# Deploy / restart NBank stack on local Minikube via Helm.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_DIR="${SCRIPT_DIR}/nbank-chart"

echo ">>> Starting Minikube (docker driver)"
minikube start --driver=docker

echo ">>> Installing / upgrading Helm release 'nbank'"
helm upgrade --install nbank "${CHART_DIR}"

echo ">>> Waiting for pods to become ready"
kubectl wait --for=condition=ready pod -l app=postgres --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=wiremock --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=backend --timeout=180s || true
kubectl wait --for=condition=ready pod -l app=frontend --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=selenoid --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=selenoid-ui --timeout=120s || true

echo ">>> Services"
kubectl get svc

echo ">>> Pods"
kubectl get pods

echo ">>> Sample backend logs"
kubectl logs deployment/backend --tail=50 || true

echo ">>> Port-forward (Ctrl+C to stop)"
echo "  frontend   http://localhost:3000"
echo "  backend    http://localhost:4111"
echo "  selenoid   http://localhost:4444"
echo "  selenoid-ui http://localhost:8080"

kubectl port-forward svc/frontend 3000:80 &
PF1=$!
kubectl port-forward svc/backend 4111:4111 &
PF2=$!
kubectl port-forward svc/selenoid 4444:4444 &
PF3=$!
kubectl port-forward svc/selenoid-ui 8080:8080 &
PF4=$!

trap 'kill $PF1 $PF2 $PF3 $PF4 2>/dev/null || true' EXIT
wait
