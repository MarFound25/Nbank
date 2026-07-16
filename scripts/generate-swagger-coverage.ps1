param(
    [string]$ApiDocsUrl = "http://localhost:4112/v3/api-docs",
    [string]$InputDir = "target/swagger-coverage-output",
    [string]$Version = "1.5.0"
)

$ErrorActionPreference = "Stop"
$zipName = "swagger-coverage-$Version.zip"
$dir = ".swagger-coverage-commandline"

if (-not (Test-Path "$dir/bin/swagger-coverage-commandline.bat") -and
    -not (Test-Path "$dir/bin/swagger-coverage-commandline")) {
    Write-Host ">>> Downloading swagger-coverage $Version"
    Invoke-WebRequest -Uri "https://github.com/viclovsky/swagger-coverage/releases/download/$Version/$zipName" -OutFile $zipName
    if (Test-Path $dir) { Remove-Item -Recurse -Force $dir }
    Expand-Archive -Path $zipName -DestinationPath "." -Force
    $extracted = Get-ChildItem -Directory -Filter "swagger-coverage*" | Select-Object -First 1
    if ($extracted -and $extracted.Name -ne $dir.TrimStart('.')) {
        Rename-Item $extracted.FullName $dir
    }
    Remove-Item $zipName -Force
}

if (-not (Test-Path $InputDir)) {
    throw "Coverage input directory not found: $InputDir. Run API tests first."
}

$cli = if (Test-Path "$dir/bin/swagger-coverage-commandline.bat") {
    "$dir/bin/swagger-coverage-commandline.bat"
} else {
    "$dir/bin/swagger-coverage-commandline"
}

Write-Host ">>> Generating Swagger coverage report"
Write-Host "    spec:  $ApiDocsUrl"
Write-Host "    input: $InputDir"
& $cli -s $ApiDocsUrl -i $InputDir
Write-Host ">>> Done. Open swagger-coverage-report.html"
