#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
  -n monitoring \
  --create-namespace \
  -f "${SCRIPT_DIR}/monitoring-values.yaml" \
  --timeout=10m

kubectl wait --for=create pod \
  -l app.kubernetes.io/name=prometheus \
  -n monitoring \
  --timeout=600s

kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=prometheus \
  -n monitoring \
  --timeout=600s

kubectl wait --for=create pod \
  -l app.kubernetes.io/name=grafana \
  -n monitoring \
  --timeout=600s

kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=grafana \
  -n monitoring \
  --timeout=600s

kubectl create secret generic backend-basic-auth \
  --from-literal=username=admin \
  --from-literal=password=admin \
  -n monitoring \
  --dry-run=client \
  -o yaml | kubectl apply -f -

kubectl apply -f "${SCRIPT_DIR}/service-monitor.yaml"
kubectl apply -f "${SCRIPT_DIR}/business-dashboard.yaml"

kubectl get pods -n monitoring
kubectl get svc -n monitoring
kubectl get servicemonitor -n monitoring

kubectl port-forward -n monitoring svc/monitoring-grafana 3001:80 &
GRAFANA_PID=$!
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090 &
PROMETHEUS_PID=$!

trap 'kill "$GRAFANA_PID" "$PROMETHEUS_PID" 2>/dev/null || true' EXIT
wait
