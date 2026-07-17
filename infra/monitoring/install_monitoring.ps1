$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ValuesFile = Join-Path $ScriptDir "monitoring-values.yaml"
$ServiceMonitorFile = Join-Path $ScriptDir "service-monitor.yaml"
$DashboardFile = Join-Path $ScriptDir "business-dashboard.yaml"

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm upgrade --install monitoring prometheus-community/kube-prometheus-stack `
  -n monitoring `
  --create-namespace `
  -f $ValuesFile `
  --timeout=10m

kubectl wait --for=create pod `
  -l app.kubernetes.io/name=prometheus `
  -n monitoring `
  --timeout=600s

kubectl wait --for=condition=ready pod `
  -l app.kubernetes.io/name=prometheus `
  -n monitoring `
  --timeout=600s

kubectl wait --for=create pod `
  -l app.kubernetes.io/name=grafana `
  -n monitoring `
  --timeout=600s

kubectl wait --for=condition=ready pod `
  -l app.kubernetes.io/name=grafana `
  -n monitoring `
  --timeout=600s

$secret = kubectl create secret generic backend-basic-auth `
  --from-literal=username=admin `
  --from-literal=password=admin `
  -n monitoring `
  --dry-run=client `
  -o yaml

$secret | kubectl apply -f -
kubectl apply -f $ServiceMonitorFile
kubectl apply -f $DashboardFile

kubectl get pods -n monitoring
kubectl get svc -n monitoring
kubectl get servicemonitor -n monitoring

$grafanaJob = Start-Job { kubectl port-forward -n monitoring svc/monitoring-grafana 3001:80 }
$prometheusJob = Start-Job { kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090 }

Write-Host "Grafana: http://localhost:3001"
Write-Host "Prometheus: http://localhost:9090"
Write-Host "Jobs: $($grafanaJob.Id), $($prometheusJob.Id)"
Write-Host "Press Enter to stop"

[void][Console]::ReadLine()
$grafanaJob, $prometheusJob | Stop-Job -PassThru | Remove-Job
