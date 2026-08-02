[CmdletBinding()]
param(
    [ValidateRange(30, 600)]
    [int]$TimeoutSeconds = 180
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

function Wait-ForCondition {
    param(
        [Parameter(Mandatory)][string]$Description,
        [Parameter(Mandatory)][scriptblock]$Condition,
        [Parameter(Mandatory)][int]$Timeout
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($Timeout)
    do {
        try {
            if (& $Condition) {
                Write-Host "[OK] $Description" -ForegroundColor Green
                return
            }
        }
        catch {
            # Startup failures are retried until the bounded deadline.
        }
        Start-Sleep -Seconds 2
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    throw "Timed out waiting for: $Description"
}

function Test-HttpEndpoint {
    param([Parameter(Mandatory)][string]$Uri)

    $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 5
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 300
}

function Assert-OneShotSucceeded {
    param(
        [Parameter(Mandatory)][string]$Service,
        [Parameter(Mandatory)][int]$Timeout
    )

    $containerId = (& docker compose --env-file $environmentFile ps --all --quiet $Service).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        throw "Could not resolve one-shot service $Service."
    }

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($Timeout)
    do {
        $containerState = (& docker inspect --format '{{.State.Status}}' $containerId).Trim()
        if ($containerState -eq 'exited') {
            break
        }
        if ($containerState -in @('dead', 'removing')) {
            throw "One-shot service $Service entered unexpected state $containerState."
        }
        Start-Sleep -Seconds 2
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    if ($containerState -ne 'exited') {
        throw "Timed out waiting for one-shot service $Service."
    }

    $exitCode = (& docker inspect --format '{{.State.ExitCode}}' $containerId).Trim()
    if ($LASTEXITCODE -ne 0 -or $exitCode -ne '0') {
        throw "One-shot service $Service did not complete successfully."
    }
    Write-Host "[OK] $Service completed successfully" -ForegroundColor Green
}

function ConvertFrom-Base64Url {
    param([Parameter(Mandatory)][string]$Value)

    $padded = $Value.Replace('-', '+').Replace('_', '/')
    switch ($padded.Length % 4) {
        2 { $padded += '==' }
        3 { $padded += '=' }
    }
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($padded))
}

if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw "Missing $environmentFile. Copy .env.example to .env and configure local secrets first."
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Required command 'docker' was not found."
}

$settings = Read-DotEnv -Path $environmentFile
$requiredSettings = @(
    'COMPOSE_PROJECT_NAME', 'POSTGRES_PORT', 'REDIS_PORT', 'KAFKA_PORT',
    'KEYCLOAK_PORT', 'KEYCLOAK_MANAGEMENT_PORT', 'MAILHOG_SMTP_PORT',
    'MAILHOG_UI_PORT', 'PROMETHEUS_PORT', 'GRAFANA_PORT', 'LOKI_PORT',
    'TEMPO_PORT', 'PRODUCT_SERVICE_PORT', 'GATEWAY_DB_NAME', 'GATEWAY_DB_USER', 'GATEWAY_DB_PASSWORD',
    'IDENTITY_DB_NAME', 'IDENTITY_DB_USER', 'IDENTITY_DB_PASSWORD',
    'PRODUCT_DB_NAME', 'PRODUCT_DB_USER', 'PRODUCT_DB_PASSWORD',
    'INVENTORY_DB_NAME', 'INVENTORY_DB_USER', 'INVENTORY_DB_PASSWORD',
    'ORDER_DB_NAME', 'ORDER_DB_USER', 'ORDER_DB_PASSWORD',
    'PAYMENT_DB_NAME', 'PAYMENT_DB_USER', 'PAYMENT_DB_PASSWORD',
    'DELIVERY_DB_NAME', 'DELIVERY_DB_USER', 'DELIVERY_DB_PASSWORD',
    'NOTIFICATION_DB_NAME', 'NOTIFICATION_DB_USER', 'NOTIFICATION_DB_PASSWORD',
    'KEYCLOAK_DB_NAME', 'KEYCLOAK_DB_USER', 'KEYCLOAK_DB_PASSWORD',
    'REDIS_PASSWORD', 'KEYCLOAK_CUSTOMER_PASSWORD',
    'GRAFANA_ADMIN_USER', 'GRAFANA_ADMIN_PASSWORD'
)
foreach ($settingName in $requiredSettings) {
    if (-not $settings.ContainsKey($settingName) -or [string]::IsNullOrWhiteSpace([string]$settings[$settingName])) {
        throw "Missing required setting $settingName in .env."
    }
}

Push-Location $repositoryRoot
try {
    $runningServices = @(& docker compose --env-file $environmentFile ps --services --status running)
    foreach ($coreService in @('postgres', 'redis', 'kafka', 'keycloak', 'mailhog')) {
        if ($runningServices -notcontains $coreService) {
            throw "Required core service $coreService is not running."
        }
        Write-Host "[OK] $coreService is running" -ForegroundColor Green
    }

    Assert-OneShotSucceeded -Service 'kafka-init' -Timeout $TimeoutSeconds
    Assert-OneShotSucceeded -Service 'keycloak-bootstrap' -Timeout $TimeoutSeconds

    $databaseMappings = @(
        @('GATEWAY_DB_NAME', 'GATEWAY_DB_USER', 'GATEWAY_DB_PASSWORD'),
        @('IDENTITY_DB_NAME', 'IDENTITY_DB_USER', 'IDENTITY_DB_PASSWORD'),
        @('PRODUCT_DB_NAME', 'PRODUCT_DB_USER', 'PRODUCT_DB_PASSWORD'),
        @('INVENTORY_DB_NAME', 'INVENTORY_DB_USER', 'INVENTORY_DB_PASSWORD'),
        @('ORDER_DB_NAME', 'ORDER_DB_USER', 'ORDER_DB_PASSWORD'),
        @('PAYMENT_DB_NAME', 'PAYMENT_DB_USER', 'PAYMENT_DB_PASSWORD'),
        @('DELIVERY_DB_NAME', 'DELIVERY_DB_USER', 'DELIVERY_DB_PASSWORD'),
        @('NOTIFICATION_DB_NAME', 'NOTIFICATION_DB_USER', 'NOTIFICATION_DB_PASSWORD'),
        @('KEYCLOAK_DB_NAME', 'KEYCLOAK_DB_USER', 'KEYCLOAK_DB_PASSWORD')
    )

    foreach ($mapping in $databaseMappings) {
        $databaseName = [string]$settings[$mapping[0]]
        $databaseUser = [string]$settings[$mapping[1]]
        $databasePassword = [string]$settings[$mapping[2]]
        & docker compose --env-file $environmentFile exec --no-TTY --env "PGPASSWORD=$databasePassword" postgres psql --host 127.0.0.1 --username $databaseUser --dbname $databaseName --tuples-only --no-align --command 'SELECT 1' *> $null
        if ($LASTEXITCODE -ne 0) {
            throw "Database connectivity failed for $databaseName with its owner principal."
        }
        Write-Host "[OK] isolated database $databaseName" -ForegroundColor Green
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker compose --env-file $environmentFile exec --no-TTY --env "PGPASSWORD=$($settings['PRODUCT_DB_PASSWORD'])" postgres psql --host 127.0.0.1 --username $settings['PRODUCT_DB_USER'] --dbname $settings['ORDER_DB_NAME'] --command 'SELECT 1' *> $null
        $crossServiceDatabaseExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($crossServiceDatabaseExitCode -eq 0) {
        throw 'Database isolation failure: the product principal connected to the order database.'
    }
    Write-Host '[OK] cross-service database access is denied' -ForegroundColor Green

    $actualTopics = @(& docker compose --env-file $environmentFile exec --no-TTY kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list)
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not list Kafka topics.'
    }
    $expectedTopics = @(
        'commerce.order.v1', 'commerce.inventory.v1', 'commerce.payment.v1',
        'commerce.delivery.v1', 'commerce.product.v1', 'commerce.order.v1.dlt',
        'commerce.inventory.v1.dlt', 'commerce.payment.v1.dlt',
        'commerce.delivery.v1.dlt', 'commerce.product.v1.dlt'
    )
    foreach ($topic in $expectedTopics) {
        if ($actualTopics -notcontains $topic) {
            throw "Missing Kafka topic $topic."
        }
    }
    Write-Host '[OK] Kafka topics and DLTs exist' -ForegroundColor Green

    $redisResult = (& docker compose --env-file $environmentFile exec --no-TTY redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping').Trim()
    if ($LASTEXITCODE -ne 0 -or $redisResult -ne 'PONG') {
        throw 'Authenticated Redis PING failed.'
    }
    Write-Host '[OK] Redis requires and accepts configured authentication' -ForegroundColor Green

    $keycloakPort = [int]$settings['KEYCLOAK_PORT']
    $keycloakManagementPort = [int]$settings['KEYCLOAK_MANAGEMENT_PORT']
    Wait-ForCondition -Description 'Keycloak readiness endpoint' -Timeout $TimeoutSeconds -Condition {
        Test-HttpEndpoint -Uri "http://127.0.0.1:$keycloakManagementPort/health/ready"
    }
    $discoveryParameters = @{
        Uri = "http://127.0.0.1:$keycloakPort/realms/order-logistics/.well-known/openid-configuration"
        TimeoutSec = 10
    }
    $discovery = Invoke-RestMethod @discoveryParameters
    if ($discovery.issuer -ne "http://localhost:$keycloakPort/realms/order-logistics") {
        throw "Unexpected Keycloak issuer $($discovery.issuer)."
    }

    $tokenParameters = @{
        Method = 'Post'
        Uri = "http://127.0.0.1:$keycloakPort/realms/order-logistics/protocol/openid-connect/token"
        ContentType = 'application/x-www-form-urlencoded'
        Body = @{
            client_id = 'platform-local-verifier'
            grant_type = 'password'
            username = 'customer@example.test'
            password = [string]$settings['KEYCLOAK_CUSTOMER_PASSWORD']
            scope = 'openid'
        }
        TimeoutSec = 10
    }
    $tokenResponse = Invoke-RestMethod @tokenParameters
    $tokenParts = $tokenResponse.access_token.Split('.')
    if ($tokenParts.Count -ne 3) {
        throw 'Keycloak returned a malformed access token.'
    }
    $claims = ConvertFrom-Json (ConvertFrom-Base64Url -Value $tokenParts[1])
    $audiences = @($claims.aud)
    $realmRoles = @($claims.realm_access.roles)
    if ($audiences -notcontains 'platform-api' -or $realmRoles -notcontains 'CUSTOMER') {
        throw 'Keycloak token is missing the platform-api audience or CUSTOMER role.'
    }
    Write-Host '[OK] OIDC discovery and role-bearing JWT claims' -ForegroundColor Green

    $mailhogSmtpPort = [int]$settings['MAILHOG_SMTP_PORT']
    $mailhogUiPort = [int]$settings['MAILHOG_UI_PORT']
    Wait-ForCondition -Description 'MailHog API endpoint' -Timeout $TimeoutSeconds -Condition {
        Test-HttpEndpoint -Uri "http://127.0.0.1:$mailhogUiPort/api/v2/messages"
    }
    $messageId = [Guid]::NewGuid().ToString('N')
    $message = [Net.Mail.MailMessage]::new(
        'verification@platform.local',
        'phase2@example.test',
        "Phase 2 verification $messageId",
        'Local infrastructure verification message.'
    )
    $smtpClient = [Net.Mail.SmtpClient]::new('127.0.0.1', $mailhogSmtpPort)
    try {
        $smtpClient.Send($message)
    }
    finally {
        $smtpClient.Dispose()
        $message.Dispose()
    }
    Wait-ForCondition -Description 'MailHog accepted a local-only email' -Timeout 30 -Condition {
        $messages = Invoke-RestMethod -Uri "http://127.0.0.1:$mailhogUiPort/api/v2/messages" -TimeoutSec 5
        return @($messages.items.Content.Headers.Subject) -contains "Phase 2 verification $messageId"
    }

    if ($runningServices -contains 'kafka-ui') {
        $kafkaUiPort = [int]$settings['KAFKA_UI_PORT']
        Wait-ForCondition -Description 'Kafka UI health endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$kafkaUiPort/actuator/health"
        }
    }

    if ($runningServices -contains 'product-service') {
        $productServicePort = [int]$settings['PRODUCT_SERVICE_PORT']
        Wait-ForCondition -Description 'Product Service readiness endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$productServicePort/actuator/health/readiness"
        }
        Wait-ForCondition -Description 'Product Service OpenAPI endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$productServicePort/v3/api-docs"
        }
    }

    if ($runningServices -contains 'prometheus') {
        foreach ($observabilityService in @('prometheus', 'loki', 'tempo', 'alloy', 'kafka-exporter', 'redis-exporter', 'postgres-exporter', 'grafana')) {
            if ($runningServices -notcontains $observabilityService) {
                throw "Observability service $observabilityService is not running."
            }
        }

        $prometheusPort = [int]$settings['PROMETHEUS_PORT']
        $lokiPort = [int]$settings['LOKI_PORT']
        $tempoPort = [int]$settings['TEMPO_PORT']
        $grafanaPort = [int]$settings['GRAFANA_PORT']

        Wait-ForCondition -Description 'Prometheus readiness endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$prometheusPort/-/ready"
        }
        Wait-ForCondition -Description 'Loki readiness endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$lokiPort/ready"
        }
        Wait-ForCondition -Description 'Tempo readiness endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$tempoPort/ready"
        }
        Wait-ForCondition -Description 'Grafana health endpoint' -Timeout $TimeoutSeconds -Condition {
            Test-HttpEndpoint -Uri "http://127.0.0.1:$grafanaPort/api/health"
        }

        $prometheusJobs = @('keycloak', 'kafka-exporter', 'redis-exporter', 'postgres-exporter', 'loki', 'tempo', 'alloy')
        if ($runningServices -contains 'product-service') {
            $prometheusJobs += 'product-service'
        }
        foreach ($prometheusJob in $prometheusJobs) {
            $queryExpression = 'up{job="' + $prometheusJob + '"} == 1'
            $query = [Uri]::EscapeDataString($queryExpression)
            Wait-ForCondition -Description "Prometheus target $prometheusJob" -Timeout $TimeoutSeconds -Condition {
                $queryResult = Invoke-RestMethod -Uri "http://127.0.0.1:$prometheusPort/api/v1/query?query=$query" -TimeoutSec 5
                return @($queryResult.data.result).Count -gt 0
            }
        }

        $credentials = '{0}:{1}' -f $settings['GRAFANA_ADMIN_USER'], $settings['GRAFANA_ADMIN_PASSWORD']
        $basicToken = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($credentials))
        $headers = @{ Authorization = "Basic $basicToken" }
        foreach ($dataSourceUid in @('prometheus', 'loki', 'tempo')) {
            $dataSourceParameters = @{
                Uri = "http://127.0.0.1:$grafanaPort/api/datasources/uid/$dataSourceUid"
                Headers = $headers
                TimeoutSec = 10
            }
            $dataSource = Invoke-RestMethod @dataSourceParameters
            if ($dataSource.uid -ne $dataSourceUid) {
                throw "Grafana datasource $dataSourceUid was not provisioned."
            }
        }
        Write-Host '[OK] Grafana datasources Prometheus, Loki and Tempo' -ForegroundColor Green
    }
}
finally {
    Pop-Location
}

Write-Host 'All applicable local environment verification checks passed.' -ForegroundColor Green
