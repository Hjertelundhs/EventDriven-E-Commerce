[CmdletBinding()]
param(
    [switch]$CoreOnly,
    [switch]$SkipVerification
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $repositoryRoot '.env'

function Read-DotEnv {
    param([Parameter(Mandatory)][string]$Path)

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }

        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            throw "Invalid .env line: $line"
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$key] = $value
    }

    return $values
}

function Assert-CommandAvailable {
    param([Parameter(Mandatory)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Install Docker Desktop with the Compose plugin."
    }
}

if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw "Missing $environmentFile. Copy .env.example to .env and replace every CHANGE_ME value."
}

$settings = Read-DotEnv -Path $environmentFile
$secretNames = @(
    'POSTGRES_SUPERUSER_PASSWORD',
    'GATEWAY_DB_PASSWORD',
    'IDENTITY_DB_PASSWORD',
    'PRODUCT_DB_PASSWORD',
    'INVENTORY_DB_PASSWORD',
    'ORDER_DB_PASSWORD',
    'PAYMENT_DB_PASSWORD',
    'DELIVERY_DB_PASSWORD',
    'NOTIFICATION_DB_PASSWORD',
    'KEYCLOAK_DB_PASSWORD',
    'MONITORING_DB_PASSWORD',
    'REDIS_PASSWORD',
    'KEYCLOAK_ADMIN_PASSWORD',
    'KEYCLOAK_CUSTOMER_PASSWORD',
    'KEYCLOAK_STAFF_PASSWORD',
    'GRAFANA_ADMIN_PASSWORD'
)

$secretValues = @()
foreach ($secretName in $secretNames) {
    if (-not $settings.ContainsKey($secretName)) {
        throw "Missing required setting $secretName in .env."
    }

    $secretValue = [string]$settings[$secretName]
    if ($secretValue -match 'CHANGE_ME' -or $secretValue.Length -lt 16) {
        throw "Setting $secretName must be replaced with a unique value of at least 16 characters."
    }
    if ($secretValue -notmatch '^[A-Za-z0-9._~-]+$') {
        throw "Setting $secretName may only contain letters, digits, dot, underscore, tilde and hyphen so Docker .env parsing remains unambiguous."
    }
    $secretValues += $secretValue
}

if (($secretValues | Select-Object -Unique).Count -ne $secretValues.Count) {
    throw 'Every local secret must have a unique value.'
}

Assert-CommandAvailable -Name 'docker'

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker is installed but the Docker daemon is not available. Start Docker Desktop and retry.'
}

& docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'The Docker Compose plugin is not available.'
}

$profiles = @('--profile', 'core')
if (-not $CoreOnly) {
    $profiles += @('--profile', 'tools', '--profile', 'observability')
}

Push-Location $repositoryRoot
try {
    & docker compose --env-file $environmentFile @profiles config --quiet
    if ($LASTEXITCODE -ne 0) {
        throw 'docker compose config validation failed.'
    }

    & docker compose --env-file $environmentFile @profiles up --detach --remove-orphans
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose failed to start the local environment.'
    }

    if (-not $SkipVerification) {
        & (Join-Path $PSScriptRoot 'verify-local.ps1')
        if ($LASTEXITCODE -ne 0) {
            throw 'Local environment verification failed.'
        }
    }
}
finally {
    Pop-Location
}

Write-Host 'Local order and logistics infrastructure is ready.' -ForegroundColor Green
