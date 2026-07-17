# Monitoring results

## Installation

```text
NAME       NAMESPACE  REVISION  STATUS    CHART                          APP VERSION
monitoring monitoring 1         deployed  kube-prometheus-stack-87.16.1 v0.92.1
```

## Pods

```text
NAME                                                     READY   STATUS
alertmanager-monitoring-kube-prometheus-alertmanager-0   2/2     Running
monitoring-grafana-7b97c5ffdf-ls694                      3/3     Running
monitoring-kube-prometheus-operator-7495d5c78b-fp9cn     1/1     Running
monitoring-kube-state-metrics-797d5dd89-zksxx            1/1     Running
monitoring-prometheus-node-exporter-7w9gc                1/1     Running
prometheus-monitoring-kube-prometheus-prometheus-0       2/2     Running
```

## Services

```text
NAME                                      TYPE        PORTS
monitoring-grafana                        NodePort    80:30300/TCP
monitoring-kube-prometheus-alertmanager   ClusterIP   9093/TCP,8080/TCP
monitoring-kube-prometheus-operator       ClusterIP   443/TCP
monitoring-kube-prometheus-prometheus     NodePort    9090:30090/TCP,8080:31934/TCP
monitoring-kube-state-metrics             ClusterIP   8080/TCP
monitoring-prometheus-node-exporter       ClusterIP   9100/TCP
```

## Application monitoring

`spring-bank-monitor` selects the `backend` service in the `default` namespace and scrapes `/actuator/prometheus` every 15 seconds.

```text
health  scrapeUrl
up      http://10.244.0.6:4111/actuator/prometheus
up      http://10.244.0.9:4111/actuator/prometheus
```

The `backend-basic-auth` Secret is stored in the `monitoring` namespace. The Secret contains the username and password used by the ServiceMonitor.

## Business metrics

```text
user_created_total
transfer_started_total
transfer_success_total
transfer_failed_total
admin_user_deleted_total
account_created_total
customer_profile_fetched_total
user_login_total
```

## Grafana

Grafana health:

```text
database=ok
version=13.1.0
```

Dashboard:

```text
NBank Business Metrics
/d/nbank-business/nbank-business-metrics
```

## Local access

```powershell
kubectl port-forward -n monitoring svc/monitoring-grafana 3001:80
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
```

- Grafana: http://localhost:3001
- Prometheus: http://localhost:9090
- Grafana credentials: `admin` / `admin`

## Reproduce

```powershell
cd infra/monitoring
.\install_monitoring.ps1
```
