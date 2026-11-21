<#
.SYNOPSIS
    Chạy bộ kiểm thử hiệu năng Apache JMeter 5.6.3 cho QNU-Events Backend và kiểm tra ngưỡng SLA (SRS §4.1).

.EXAMPLE
    # Chạy kịch bản 01 (Baseline Load) trong 30s với 20 VU
    .\run-jmeter.ps1 -Plan 01 -Threads 20 -Duration 30

    # Chạy kịch bản 02 (Registration Quota Burst: 30 SV tranh 5 suất)
    .\run-jmeter.ps1 -Plan 02 -BurstUsers 30 -QuotaTotal 5

    # Sinh CSV 100 SV cho P1 và chạy kịch bản 03 (P1 Dynamic QR Check-in)
    .\run-jmeter.ps1 -Plan 03 -GenerateCsvCount 100 -P1CheckInUsers 100 -P1ReadUsers 50 -Duration 60

    # Chạy toàn bộ 4 kịch bản
    .\run-jmeter.ps1 -Plan all
#>
param(
    [ValidateSet("01", "02", "03", "04", "all")]
    [string]$Plan = "01",
    [string]$HostName = "localhost",
    [int]$Port = 8080,
    [string]$Protocol = "http",
    [int]$Threads = 20,
    [int]$Duration = 60,
    [int]$BurstUsers = 30,
    [int]$QuotaTotal = 5,
    [int]$P1CheckInUsers = 30,
    [int]$P1ReadUsers = 20,
    [int]$TargetRpm = 6000,
    [int]$GenerateCsvCount = 0
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jmeterRoot = Split-Path -Parent $scriptDir
$beRoot = (Resolve-Path (Join-Path $jmeterRoot "..\..\..")).Path
$plansDir = Join-Path $jmeterRoot "plans"
$dataDir = Join-Path $jmeterRoot "data"
$configFile = Join-Path $jmeterRoot "config\user.properties"
$outRoot = Join-Path $beRoot "target\jmeter"

if ($GenerateCsvCount -gt 0) {
    $csvPath = Join-Path $dataDir "perf_students.csv"
    Write-Host "[DATA] Đang tạo $csvPath với $GenerateCsvCount tài khoản perf_sv_0001..perf_sv_$($GenerateCsvCount.ToString('0000'))..." -ForegroundColor Cyan
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("loginId,password,classId")
    for ($i = 1; $i -le $GenerateCsvCount; $i++) {
        $lines.Add(("perf_sv_{0:D4},Demo@12345,00000000-0000-0000-0000-000000000001" -f $i))
    }
    [System.IO.File]::WriteAllLines($csvPath, $lines, [System.Text.UTF8Encoding]::new($false))
    Write-Host "[DATA] Hoàn tất! Nhớ chạy prepare-perf-data.sql trên PostgreSQL với -v student_count=$GenerateCsvCount." -ForegroundColor Green
}

# Health check Backend trước khi bắn tải
$healthUrl = "${Protocol}://${HostName}:${Port}/actuator/health"
Write-Host "[CHECK] Kiểm tra Backend tại $healthUrl ..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 5
    Write-Host "[OK] Backend UP (status=$($health.status))" -ForegroundColor Green
} catch {
    Write-Error "Không thể kết nối Backend tại $healthUrl. Hãy khởi động PostgreSQL và chạy '.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev' trước."
    exit 1
}

$planMap = @{
    "01" = "01-baseline-read-and-auth-load.jmx"
    "02" = "02-registration-quota-burst.jmx"
    "03" = "03-p1-dynamic-qr-checkin-load.jmx"
    "04" = "04-stress-and-spike-test.jmx"
}

$selectedPlans = if ($Plan -eq "all") {
    @("01", "02", "03", "04") | ForEach-Object { $planMap[$_] }
} else {
    @($planMap[$Plan])
}

$jmeterCmd = Get-Command jmeter -ErrorAction SilentlyContinue
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

function Show-SlaSummary([string]$CsvResultPath) {
    if (-not (Test-Path $CsvResultPath)) { return }
    $rows = Import-Csv $CsvResultPath
    if (-not $rows -or $rows.Count -eq 0) { return }

    $total = $rows.Count
    $failed = ($rows | Where-Object { $_.success -ne "true" }).Count
    $errPct = [Math]::Round(($failed * 100.0) / $total, 2)
    $times = ($rows | ForEach-Object { [double]$_.elapsed }) | Sort-Object
    $avg = [Math]::Round(($times | Measure-Object -Average).Average, 1)
    $p95Idx = [Math]::Min($total - 1, [int][Math]::Ceiling(0.95 * $total) - 1)
    $p99Idx = [Math]::Min($total - 1, [int][Math]::Ceiling(0.99 * $total) - 1)
    $p95 = [Math]::Round($times[$p95Idx], 1)
    $p99 = [Math]::Round($times[$p99Idx], 1)

    $minTs = ($rows | ForEach-Object { [long]$_.timeStamp } | Measure-Object -Minimum).Minimum
    $maxTs = ($rows | ForEach-Object { [long]$_.timeStamp } | Measure-Object -Maximum).Maximum
    $sec = [Math]::Max(1.0, ($maxTs - $minTs) / 1000.0)
    $rps = [Math]::Round($total / $sec, 2)

    Write-Host "`n================ KẾT QUẢ SLA (SRS §4.1) ================" -ForegroundColor Yellow
    Write-Host ("  Tổng mẫu (Samples) : {0}" -f $total)
    Write-Host ("  Thông lượng (RPS)  : {0} req/s" -f $rps)
    Write-Host ("  Tỷ lệ lỗi (Error%) : {0}%  (Ngưỡng NFR-PERF-001 <= 1%)" -f $errPct) -ForegroundColor ($(if ($errPct -le 1.0) { "Green" } else { "Red" }))
    Write-Host ("  Độ trễ Trung bình  : {0} ms" -f $avg)
    Write-Host ("  Độ trễ p95         : {0} ms (Ngưỡng NFR-PERF-002 <= 2000 ms)" -f $p95) -ForegroundColor ($(if ($p95 -le 2000) { "Green" } else { "Red" }))
    Write-Host ("  Độ trễ p99         : {0} ms (Ngưỡng NFR-PERF-002 <= 5000 ms)" -f $p99) -ForegroundColor ($(if ($p99 -le 5000) { "Green" } else { "Red" }))
    Write-Host "========================================================`n" -ForegroundColor Yellow
}

foreach ($planFile in $selectedPlans) {
    $planPath = Join-Path $plansDir $planFile
    $planBase = [System.IO.Path]::GetFileNameWithoutExtension($planFile)
    $jtlFile = Join-Path $outRoot "results\${planBase}_${timestamp}.csv"
    $reportDir = Join-Path $outRoot "reports\${planBase}_${timestamp}"
    New-Item -ItemType Directory -Force -Path (Split-Path $jtlFile) | Out-Null

    Write-Host "[RUN] Thực thi $planFile ..." -ForegroundColor Cyan

    if ($jmeterCmd) {
        & jmeter -n `
            -t $planPath `
            -q $configFile `
            -l $jtlFile `
            -e -o $reportDir `
            "-Jhost=$HostName" `
            "-Jport=$Port" `
            "-Jprotocol=$Protocol" `
            "-JdataDir=$dataDir" `
            "-Jthreads=$Threads" `
            "-Jduration=$Duration" `
            "-JburstUsers=$BurstUsers" `
            "-JquotaTotal=$QuotaTotal" `
            "-Jp1CheckInUsers=$P1CheckInUsers" `
            "-Jp1ReadUsers=$P1ReadUsers" `
            "-JtargetRpm=$TargetRpm"

        Show-SlaSummary $jtlFile
        Write-Host "[REPORT] Mở báo cáo HTML Dashboard: $reportDir\index.html" -ForegroundColor Green
    } else {
        Write-Host "[INFO] Không thấy 'jmeter' trên PATH -> Chuyển sang chạy qua Maven (jmeter-maven-plugin 5.6.3)..." -ForegroundColor Yellow
        Push-Location $beRoot
        try {
            & .\mvnw.cmd -B -ntp verify -Pjmeter-test `
                "-Djmeter.testFile=$planFile" `
                "-Djmeter.host=$HostName" `
                "-Djmeter.port=$Port" `
                "-Djmeter.protocol=$Protocol" `
                "-DskipTests=true"
        } finally {
            Pop-Location
        }
        $latestCsv = Get-ChildItem -Path (Join-Path $outRoot "results") -Filter "*.csv" -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($latestCsv) {
            Show-SlaSummary $latestCsv.FullName
        }
        Write-Host "[REPORT] Xem HTML Dashboard tại thư mục: $outRoot\reports\" -ForegroundColor Green
    }
}
