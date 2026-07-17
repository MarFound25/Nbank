# Deploy / restart NBank stack on local Minikube via Helm (Windows).
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ChartDir = Join-Path $ScriptDir "nbank-chart"

Write-Host ">>> Starting Minikube (docker driver)"
minikube start --driver=docker

Write-Host ">>> Installing / upgrading Helm release 'nbank'"
helm upgrade --install nbank $ChartDir

Write-Host ">>> Waiting for pods"
foreach ($app in @("postgres","wiremock","backend","frontend","selenoid","selenoid-ui")) {
  kubectl wait --for=condition=ready "pod" -l "app=$app" --timeout=180s 2>$null
}

Write-Host ">>> Services"
kubectl get svc
Write-Host ">>> Pods"
kubectl get pods
Write-Host ">>> Sample backend logs"
kubectl logs deployment/backend --tail=50

Write-Host ">>> Starting port-forwards in background jobs"
Write-Host "  frontend    http://localhost:3000"
Write-Host "  backend     http://localhost:4111"
Write-Host "  selenoid    http://localhost:4444"
Write-Host "  selenoid-ui http://localhost:8080"

$jobs = @(
  Start-Job { kubectl port-forward svc/frontend 3000:80 },
  Start-Job { kubectl port-forward svc/backend 4111:4111 },
  Start-Job { kubectl port-forward svc/selenoid 4444:4444 },
  Start-Job { kubectl port-forward svc/selenoid-ui 8080:8080 }
)

Write-Host "Port-forward jobs started (Ids: $($jobs.Id -join ', ')). Press Enter to stop..."
[void][Console]::ReadLine()
$jobs | Stop-Job -PassThru | Remove-Job
Write-Host "Stopped."
