<#
.SYNOPSIS
    Chạy bộ Bruno API Automation Test cho QNU-Events (ánh xạ đầy đủ 4.1 -> 4.6).
.EXAMPLE
    .\run-bruno.ps1                          # Chạy core (01 -> 06, không cần chuẩn bị DB thêm)
    .\run-bruno.ps1 -Mode full               # Chuẩn bị DB fixture + chạy đủ 01 -> 07 + dọn dẹp
    .\run-bruno.ps1 -Mode fast               # Bỏ qua 2 ca chờ đồng hồ thật (TC-QR-11/12)
    .\run-bruno.ps1 -Folder 04-Registration  # Chạy 01-Auth + riêng 1 thư mục cụ thể
#>
[CmdletBinding()]
param(
    [ValidateSet("core", "full", "fast")]
    [string]$Mode = "core",
    [string]$Folder = "",
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$PrepareDb,
    [switch]$CleanupDb,
    [switch]$SkipReports
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$collectionDir = Split-Path -Parent $scriptDir
$beRoot = (Resolve-Path (Join-Path $collectionDir "..\..\..\..")).Path
$reportDir = Join-Path $beRoot "target\bruno"

function Read-DotEnvValue {
    param([string]$Key, [string]$DefaultValue = "")
    foreach ($candidate in @((Join-Path $beRoot ".env"), (Join-Path $beRoot ".env.example"))) {
        if (Test-Path $candidate) {
            $line = Get-Content $candidate | Where-Object { $_ -match "^\s*$Key\s*=" } | Select-Object -First 1
            if ($line) {
                return ($line -split "=", 2)[1].Trim().Trim('"').Trim("'")
            }
        }
    }
    return $DefaultValue
}

if ($Mode -eq "full") {
    $PrepareDb = $true
}

# 1. Kiểm tra Backend đang chạy
Write-Host ">>> Kiểm tra Backend tại $BaseUrl/actuator/health ..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
    if ($health.status -ne "UP") {
        throw "Backend chưa ở trạng thái UP: $($health | ConvertTo-Json -Compress)"
    }
    Write-Host "    Backend UP!" -ForegroundColor Green
} catch {
    Write-Error "Không kết nối được Backend tại $BaseUrl. Hãy khởi động PostgreSQL và chạy: .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev"
    exit 1
}

# 2. Chuẩn bị DB fixture nếu yêu cầu (-Mode full hoặc -PrepareDb)
if ($PrepareDb) {
    Write-Host ">>> Nạp DB fixture (prepare-bruno-data.sql) cho thư mục 07-DB-Fixture-Cases ..." -ForegroundColor Cyan
    $sqlFile = Join-Path $scriptDir "prepare-bruno-data.sql"
    Get-Content $sqlFile -Raw | docker compose -f (Join-Path $beRoot "compose.yaml") exec -T postgres psql -U qnu_events -d qnu_events
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Nạp prepare-bruno-data.sql thất bại!"
        exit 1
    }
}

# 3. Chuẩn bị tham số chạy Bruno CLI (@usebruno/cli)
$jwtSecret = Read-DotEnvValue -Key "JWT_SECRET"
$qrSecret = Read-DotEnvValue -Key "QR_SIGNING_SECRET"

$bruArgs = @("--yes", "@usebruno/cli", "run")

if ($Folder) {
    if ($Folder -ne "01-Auth") {
        $bruArgs += "01-Auth"
    }
    $bruArgs += $Folder
    $bruArgs += "-r"
} else {
    $bruArgs += "-r"
}

$bruArgs += @("--env", "local", "--env-var", "baseUrl=$BaseUrl")
if ($jwtSecret) {
    $bruArgs += @("--env-var", "jwtSecret=$jwtSecret")
}
if ($qrSecret) {
    $bruArgs += @("--env-var", "qrSigningSecret=$qrSecret")
}

# Bảo mật R7: ẩn header Authorization trong báo cáo kết quả
$bruArgs += @("--reporter-skip-headers", "Authorization")

if ($Mode -eq "core" -and -not $Folder) {
    $bruArgs += @("--exclude-tags", "db-fixture")
} elseif ($Mode -eq "fast" -and -not $Folder) {
    $bruArgs += @("--exclude-tags", "db-fixture,slow")
}

if (-not $SkipReports) {
    New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
    $bruArgs += @(
        "--reporter-html", (Join-Path $reportDir "bruno-report.html"),
        "--reporter-junit", (Join-Path $reportDir "bruno-junit.xml"),
        "--reporter-json", (Join-Path $reportDir "bruno-results.json")
    )
}

Write-Host ">>> Thực thi Bruno CLI (Mode=$Mode) ..." -ForegroundColor Cyan
Push-Location $collectionDir
try {
    & npx @bruArgs
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

# 4. Dọn dẹp DB nếu bật -CleanupDb
if ($CleanupDb) {
    Write-Host ">>> Dọn dẹp dữ liệu [BRU-*] (cleanup-bruno-data.sql) ..." -ForegroundColor Cyan
    $cleanSql = Join-Path $scriptDir "cleanup-bruno-data.sql"
    Get-Content $cleanSql -Raw | docker compose -f (Join-Path $beRoot "compose.yaml") exec -T postgres psql -U qnu_events -d qnu_events
}

if (-not $SkipReports) {
    Write-Host ">>> Báo cáo Bruno đã xuất tại: $reportDir" -ForegroundColor Yellow
}

exit $exitCode
