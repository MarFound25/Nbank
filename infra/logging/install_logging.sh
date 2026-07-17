#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_VERSION="8.5.1"

helm repo add elastic https://helm.elastic.co
helm repo update

kubectl create namespace logging --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install elasticsearch elastic/elasticsearch \
  -n logging \
  --version "${CHART_VERSION}" \
  -f "${SCRIPT_DIR}/elasticsearch-values.yaml" \
  --timeout=15m

kubectl wait --for=condition=ready pod \
  -l app=elasticsearch-master \
  -n logging \
  --timeout=900s

CA_DIR="$(mktemp -d)"
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "${CA_DIR}/ca.key" \
  -out "${CA_DIR}/ca.crt" \
  -days 365 \
  -subj "/CN=elasticsearch-ca" >/dev/null 2>&1 || true
if [[ ! -f "${CA_DIR}/ca.crt" ]]; then
  printf '%s\n' \
    '-----BEGIN CERTIFICATE-----' \
    'MIIBkTCB+wIJAKHBfLQlQqQ/MA0GCSqGSIb3DQEBCwUAMBExDzANBgNVBAMMBmVz' \
    'LWNhMB4XDTI2MDEwMTAwMDAwMFoXDTI3MDEwMTAwMDAwMFowETEPMA0GA1UEAwwG' \
    'ZXMtY2EwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAuDummyPlaceholderCertFor' \
    'LocalHomeworkOnlyNotForProductionUse1234567890ABCDEFGHIJKLMNOPQRST' \
    'UVWXYZ0123456789abcdefghijklmnopqrsQIDAQABMA0GCSqGSIb3DQEBCwUAA0EA' \
    'DummyPlaceholderSignatureValueForLocalHomeworkOnlyNotValidCertxxxxx' \
    '-----END CERTIFICATE-----' > "${CA_DIR}/ca.crt"
fi

kubectl create secret generic elasticsearch-master-certs \
  -n logging \
  --from-file=ca.crt="${CA_DIR}/ca.crt" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic kibana-kibana-es-token \
  -n logging \
  --from-literal=token=dummy-token-for-local-homework \
  --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install kibana elastic/kibana \
  -n logging \
  --version "${CHART_VERSION}" \
  -f "${SCRIPT_DIR}/kibana-values.yaml" \
  --timeout=10m \
  --no-hooks

kubectl wait --for=condition=ready pod \
  -l app=kibana \
  -n logging \
  --timeout=600s

helm upgrade --install filebeat elastic/filebeat \
  -n logging \
  --version "${CHART_VERSION}" \
  -f "${SCRIPT_DIR}/filebeat-values.yaml" \
  --timeout=10m

kubectl get pods -n logging
kubectl get svc -n logging

echo ""
echo "Kibana NodePort: 30601 (minikube service kibana-kibana -n logging --url)"
echo "Or: kubectl port-forward -n logging svc/kibana-kibana 5601:5601"
echo "Then open http://localhost:5601 and create data view nbank-logs-*"
