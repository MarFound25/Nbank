# Logging results

## Stack

Helm charts version **8.5.1** (namespace `logging`):

- elasticsearch
- kibana (`--no-hooks`, security off on ES)
- filebeat (DaemonSet → index `nbank-logs-*`)

## Access (Windows / Minikube Docker)

NodePort часто недоступен. Использовать:

```powershell
kubectl port-forward -n logging svc/kibana-kibana 5601:5601
kubectl port-forward -n logging svc/elasticsearch-master 9200:9200
```

- Kibana: http://localhost:5601
- Data view: `nbank-logs-*` (time field `@timestamp`)

## Install

```powershell
cd infra/logging
.\install_logging.ps1
```

## Verify

```powershell
kubectl get pods -n logging
kubectl exec -n logging elasticsearch-master-0 -- curl -s http://127.0.0.1:9200/_cat/indices/*nbank*?v
```

Useful Discover queries:

- `message: "Transfer successful"`
- `message: "Transfer failed"`
- `message: "Transfer rejected due to limit"`
- `message: "Login successful for user"`
