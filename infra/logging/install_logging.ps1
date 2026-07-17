$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ChartVersion = "8.5.1"

helm repo add elastic https://helm.elastic.co
helm repo update

kubectl create namespace logging --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install elasticsearch elastic/elasticsearch `
  -n logging `
  --version $ChartVersion `
  -f (Join-Path $ScriptDir "elasticsearch-values.yaml") `
  --timeout=15m

kubectl wait --for=condition=ready pod `
  -l app=elasticsearch-master `
  -n logging `
  --timeout=900s

$caDir = Join-Path $env:TEMP "nbank-es-ca"
New-Item -ItemType Directory -Force -Path $caDir | Out-Null
$cert = New-SelfSignedCertificate -DnsName "elasticsearch-ca" -CertStoreLocation "Cert:\CurrentUser\My" -KeyExportPolicy Exportable -NotAfter (Get-Date).AddYears(2)
$bytes = $cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert)
$b64 = [Convert]::ToBase64String($bytes)
$pem = "-----BEGIN CERTIFICATE-----`n"
for ($i = 0; $i -lt $b64.Length; $i += 64) {
    $len = [Math]::Min(64, $b64.Length - $i)
    $pem += $b64.Substring($i, $len) + "`n"
}
$pem += "-----END CERTIFICATE-----`n"
Set-Content -Path (Join-Path $caDir "ca.crt") -Value $pem -NoNewline -Encoding ascii
kubectl create secret generic elasticsearch-master-certs `
  -n logging `
  --from-file=ca.crt=(Join-Path $caDir "ca.crt") `
  --dry-run=client -o yaml | kubectl apply -f -
Get-ChildItem Cert:\CurrentUser\My | Where-Object { $_.Thumbprint -eq $cert.Thumbprint } | Remove-Item

kubectl create secret generic kibana-kibana-es-token `
  -n logging `
  --from-literal=token=dummy-token-for-local-homework `
  --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install kibana elastic/kibana `
  -n logging `
  --version $ChartVersion `
  -f (Join-Path $ScriptDir "kibana-values.yaml") `
  --timeout=10m `
  --no-hooks

kubectl wait --for=condition=ready pod `
  -l app=kibana `
  -n logging `
  --timeout=600s

helm upgrade --install filebeat elastic/filebeat `
  -n logging `
  --version $ChartVersion `
  -f (Join-Path $ScriptDir "filebeat-values.yaml") `
  --timeout=10m

kubectl get pods -n logging
kubectl get svc -n logging

Write-Host ""
Write-Host "Kibana NodePort: 30601 (minikube service kibana-kibana -n logging --url)"
Write-Host "Or: kubectl port-forward -n logging svc/kibana-kibana 5601:5601"
Write-Host "Then open http://localhost:5601 and create data view nbank-logs-*"
