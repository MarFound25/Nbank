param(
    [string]$BaseUrl = "",
    [int]$DurationMinutes = 30,
    [int]$IntervalSeconds = 3
)

$ErrorActionPreference = "Continue"

function Get-BaseUrl {
    param([string]$Configured)
    if ($Configured) { return $Configured.TrimEnd("/") }
    try {
        $url = (& minikube service backend --url 2>$null | Select-Object -First 1)
        if ($url) { return $url.TrimEnd("/") }
    } catch {}
    return "http://127.0.0.1:4111"
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )
    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
        ContentType = "application/json"
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Compress -Depth 6)
    }
    try {
        return Invoke-RestMethod @params
    } catch {
        return $null
    }
}

function New-Password {
    return ("Aa1!" + ([guid]::NewGuid().ToString("N").Substring(0, 8)))
}

function New-Username {
    param([string]$Prefix)
    $suffix = ([guid]::NewGuid().ToString("N").Substring(0, 6))
    $name = ($Prefix + $suffix)
    if ($name.Length -gt 15) { $name = $name.Substring(0, 15) }
    return $name
}

function Get-AdminAuthHeader {
    param([string]$Base)
    $login = Invoke-Json -Method POST -Url "$Base/api/v1/auth/login" -Body @{
        username = "admin"
        password = "admin"
    }
    if (-not $login) { throw "Admin login failed" }
    if ($login.authHeader) { return $login.authHeader }
    if ($login.token) { return $login.token }
    return ("Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin")))
}

function Create-User {
    param([string]$Base, [string]$AdminAuth, [string]$Username, [string]$Password)
    return Invoke-Json -Method POST -Url "$Base/api/v1/admin/users" -Headers @{ Authorization = $AdminAuth } -Body @{
        username = $Username
        password = $Password
        role = "USER"
    }
}

function Login-User {
    param([string]$Base, [string]$Username, [string]$Password)
    $resp = Invoke-Json -Method POST -Url "$Base/api/v1/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    if (-not $resp) { return $null }
    if ($resp.authHeader) { return $resp.authHeader }
    if ($resp.token) { return $resp.token }
    return ("Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$Username`:$Password")))
}

function Create-Account {
    param([string]$Base, [string]$Token)
    return Invoke-Json -Method POST -Url "$Base/api/v1/accounts" -Headers @{ Authorization = $Token }
}

function Deposit {
    param([string]$Base, [string]$Token, [int]$AccountId, [double]$Amount)
    return Invoke-Json -Method POST -Url "$Base/api/v1/accounts/deposit" -Headers @{ Authorization = $Token } -Body @{
        id = $AccountId
        balance = $Amount
    }
}

function Transfer {
    param([string]$Base, [string]$Token, [int]$From, [int]$To, [double]$Amount)
    return Invoke-Json -Method POST -Url "$Base/api/v1/accounts/transfer" -Headers @{ Authorization = $Token } -Body @{
        senderAccountId = $From
        receiverAccountId = $To
        amount = $Amount
    }
}

function Get-Profile {
    param([string]$Base, [string]$Token)
    return Invoke-Json -Method GET -Url "$Base/api/v1/customer/profile" -Headers @{ Authorization = $Token }
}

function Update-Profile {
    param([string]$Base, [string]$Token, [string]$Name)
    return Invoke-Json -Method PUT -Url "$Base/api/v1/customer/profile" -Headers @{ Authorization = $Token } -Body @{
        name = $Name
    }
}

function Get-Accounts {
    param([string]$Base, [string]$Token)
    return Invoke-Json -Method GET -Url "$Base/api/v1/customer/accounts" -Headers @{ Authorization = $Token }
}

function Get-Transactions {
    param([string]$Base, [string]$Token, [int]$AccountId)
    return Invoke-Json -Method GET -Url "$Base/api/v1/accounts/$AccountId/transactions" -Headers @{ Authorization = $Token }
}

function List-Users {
    param([string]$Base, [string]$AdminAuth)
    return Invoke-Json -Method GET -Url "$Base/api/v1/admin/users" -Headers @{ Authorization = $AdminAuth }
}

function Delete-User {
    param([string]$Base, [string]$AdminAuth, [int]$UserId)
    return Invoke-Json -Method DELETE -Url "$Base/api/v1/admin/users/$UserId" -Headers @{ Authorization = $AdminAuth }
}

$Base = Get-BaseUrl -Configured $BaseUrl
Write-Host "Base URL: $Base"
Write-Host "Duration: $DurationMinutes min, interval: $IntervalSeconds sec"

$adminAuth = Get-AdminAuthHeader -Base $Base
$users = @()
$deadline = (Get-Date).AddMinutes($DurationMinutes)
$tick = 0

while ((Get-Date) -lt $deadline) {
    $tick++
    Write-Host "=== tick $tick $(Get-Date -Format 'HH:mm:ss') ==="

    $username = New-Username -Prefix "u"
    $password = New-Password
    $created = Create-User -Base $Base -AdminAuth $adminAuth -Username $username -Password $password
    if ($created) {
        $token = Login-User -Base $Base -Username $username -Password $password
        if ($token) {
            $account = Create-Account -Base $Base -Token $token
            $accountId = $null
            if ($account -and $account.id) { $accountId = [int]$account.id }
            if ($accountId) {
                Deposit -Base $Base -Token $token -AccountId $accountId -Amount (Get-Random -Minimum 200 -Maximum 2000) | Out-Null
            }
            $users += [pscustomobject]@{
                Username = $username
                Password = $password
                Token = $token
                AccountId = $accountId
                LoginCount = 1
                ProfileUpdates = 0
            }
        }
    }

    for ($i = 0; $i -lt 3; $i++) {
        $bad = Create-User -Base $Base -AdminAuth $adminAuth -Username "ab" -Password "weak"
        if (-not $bad) { Write-Host "expected user create failure #$($i+1)" }
    }

    if ($users.Count -gt 0) {
        $activeIdx = Get-Random -Minimum 0 -Maximum ([Math]::Min(5, $users.Count))
        $active = $users[$activeIdx]
        $active.Token = Login-User -Base $Base -Username $active.Username -Password $active.Password
        if ($active.Token) {
            $active.LoginCount++
            Get-Profile -Base $Base -Token $active.Token | Out-Null
            Update-Profile -Base $Base -Token $active.Token -Name ("Name " + (Get-Random -Maximum 9999)) | Out-Null
            $active.ProfileUpdates++
            Get-Accounts -Base $Base -Token $active.Token | Out-Null
            if ($active.AccountId) {
                Get-Transactions -Base $Base -Token $active.Token -AccountId $active.AccountId | Out-Null
            }
        }

        if ($users.Count -ge 2) {
            $from = $users | Where-Object { $_.AccountId -and $_.Token } | Get-Random
            $to = $users | Where-Object { $_.AccountId -and $_.Username -ne $from.Username } | Get-Random
            if ($from -and $to) {
                Transfer -Base $Base -Token $from.Token -From $from.AccountId -To $to.AccountId -Amount ([Math]::Round((Get-Random -Minimum 10 -Maximum 150) + 0.5, 2)) | Out-Null
                Transfer -Base $Base -Token $from.Token -From $from.AccountId -To $to.AccountId -Amount 999999 | Out-Null
                Transfer -Base $Base -Token $from.Token -From $from.AccountId -To $to.AccountId -Amount -50 | Out-Null
            }
        }

        if (($tick % 5) -eq 0) {
            Write-Host "transfer error burst"
            $from = $users | Where-Object { $_.AccountId -and $_.Token } | Select-Object -First 1
            if ($from) {
                1..8 | ForEach-Object {
                    Transfer -Base $Base -Token $from.Token -From $from.AccountId -To 999999 -Amount 100 | Out-Null
                }
            }
        }
    }

    List-Users -Base $Base -AdminAuth $adminAuth | Out-Null

    if (($tick % 7) -eq 0 -and $users.Count -gt 3) {
        $victim = $users[-1]
        $all = List-Users -Base $Base -AdminAuth $adminAuth
        $id = $null
        if ($all) {
            $match = $all | Where-Object { $_.username -eq $victim.Username } | Select-Object -First 1
            if ($match -and $match.id) { $id = [int]$match.id }
        }
        if ($id) {
            Delete-User -Base $Base -AdminAuth $adminAuth -UserId $id | Out-Null
            $users = @($users | Where-Object { $_.Username -ne $victim.Username })
        }
    }

    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "Activity finished"
$users | Sort-Object LoginCount -Descending | Select-Object -First 5 Username,LoginCount,ProfileUpdates | Format-Table -AutoSize
