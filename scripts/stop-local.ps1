[CmdletBinding()]
param(
    [switch]$RemoveVolumes,
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $repositoryRoot '.env'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Required command 'docker' was not found."
}

if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw "Missing $environmentFile. The project cannot be resolved safely without its environment file."
}

$projectNameLine = Get-Content -LiteralPath $environmentFile -Encoding UTF8 |
    Where-Object { $_ -match '^\s*COMPOSE_PROJECT_NAME\s*=' } |
    Select-Object -First 1

if (-not $projectNameLine) {
    throw 'COMPOSE_PROJECT_NAME is missing from .env.'
}

$projectName = ($projectNameLine -split '=', 2)[1].Trim().Trim('"').Trim("'")
if ($projectName -notmatch '^[a-z0-9][a-z0-9_-]{2,62}$') {
    throw "Unsafe or invalid COMPOSE_PROJECT_NAME '$projectName'."
}

if ($RemoveVolumes -and -not $Force) {
    $confirmation = Read-Host "This permanently deletes Docker volumes owned by project '$projectName'. Type DELETE-$projectName to continue"
    if ($confirmation -cne "DELETE-$projectName") {
        throw 'Volume deletion cancelled.'
    }
}

$arguments = @(
    'compose', '--env-file', $environmentFile,
    '--profile', 'core', '--profile', 'apps', '--profile', 'tools', '--profile', 'observability',
    'down', '--remove-orphans'
)
if ($RemoveVolumes) {
    $arguments += '--volumes'
}

Push-Location $repositoryRoot
try {
    & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose failed to stop the local environment.'
    }
}
finally {
    Pop-Location
}

if ($RemoveVolumes) {
    Write-Host "Stopped '$projectName' and permanently removed its named volumes." -ForegroundColor Yellow
}
else {
    Write-Host "Stopped '$projectName'. Named volumes were preserved." -ForegroundColor Green
}
