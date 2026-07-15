$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Users\samay\.jdks\openjdk-22.0.2"
$env:Path = "$env:JAVA_HOME\bin;D:\apache-maven-3.9.6-bin\apache-maven-3.9.6\bin;" + $env:Path
Set-Location "d:\AQA homework\first_programm\Nbank"

$results = Join-Path (Get-Location) "target\allure-results"
$report = Join-Path (Get-Location) "target\allure-report"
$historyStore = Join-Path (Get-Location) "target\allure-history-store"

Remove-Item $historyStore -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $historyStore | Out-Null

for ($i = 1; $i -le 5; $i++) {
    Write-Host "=== UI run $i / 5 (with Allure history) ===" -ForegroundColor Cyan

    Remove-Item $results -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $results | Out-Null

    if (Test-Path "$historyStore\history") {
        Copy-Item "$historyStore\history" "$results\history" -Recurse -Force
        Write-Host "Copied history into allure-results"
    }

    mvn -B -q test -Pui "-Dapi.base.url=http://localhost:4112" "-Dallure.results.directory=$results"
    if ($LASTEXITCODE -ne 0) {
        throw "UI run $i failed with exit code $LASTEXITCODE"
    }

    Remove-Item $report -Recurse -Force -ErrorAction SilentlyContinue
    mvn -B -q allure:report
    if ($LASTEXITCODE -ne 0) {
        throw "allure:report failed on run $i"
    }

    if (-not (Test-Path "$report\history")) {
        throw "Report history folder was not generated on run $i"
    }

    Remove-Item "$historyStore\history" -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item "$report\history" "$historyStore\history" -Recurse -Force
    Write-Host "=== UI run $i PASSED, history saved ===" -ForegroundColor Green
}

Write-Host "Done. Open report with: mvn allure:serve" -ForegroundColor Green
Write-Host "Trend should show 5 runs in Overview."
