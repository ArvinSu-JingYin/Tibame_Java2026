<#
.SYNOPSIS
    日常流水帳系統 (Daily Ledger System) - 智慧啟動與維運腳本

.DESCRIPTION
    支援 H2 記憶體模式與 MS SQL Server 雙設定檔切換、通訊埠自訂、智慧前置診斷、
    JVM 遠端除錯、自動開啟瀏覽器與互動式選單。

.EXAMPLE
    .\start.ps1
    預設以 H2 記憶體資料庫啟動 (Port 8080)

.EXAMPLE
    .\start.ps1 -Profile mssql -OpenBrowser
    切換至 MS SQL Server 並在伺服器就緒後自動開啟瀏覽器

.EXAMPLE
    .\start.ps1 -Port 9090 -Clean
    指定 Port 9090 並在啟動前先執行 mvn clean
#>

[CmdletBinding(DefaultParameterSetName = 'Run')]
param(
    # Spring Boot Active Profile ('h2' | 'mssql' | 'prod')
    [Alias('p', 'Mode')]
    [ValidateSet('h2', 'mssql', 'prod', IgnoreCase = $true)]
    [string]$Profile = 'h2',

    # HTTP 伺服器通訊埠 (預設 8080)
    [Alias('serverPort', 'httpPort')]
    [int]$Port = 8080,

    # 啟動前是否先執行 Maven clean
    [Alias('c')]
    [switch]$Clean,

    # 開啟 JVM 遠端除錯埠 (Port 5005)
    [Alias('d')]
    [switch]$DebugMode,

    # 伺服器就緒後自動開啟預設瀏覽器
    [Alias('b')]
    [switch]$OpenBrowser,

    # 開啟互動式終端選單
    [Alias('i')]
    [switch]$Interactive,

    # 略過前置診斷檢查 (Java / Port / DB 連線)
    [switch]$SkipCheck,

    # 透傳額外參數給 Maven / Spring Boot
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ExtraArgs
)

# 確保輸出編碼為 UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# 定義文字顏色輔助函式
function Write-Header {
    param([string]$Text)
    Write-Host "`n=== $Text ===" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Text)
    Write-Host " [OK] $Text" -ForegroundColor Green
}

function Write-Info {
    param([string]$Text)
    Write-Host " [INFO] $Text" -ForegroundColor Gray
}

function Write-Warn {
    param([string]$Text)
    Write-Host " [WARN] $Text" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Text)
    Write-Host " [ERROR] $Text" -ForegroundColor Red
}

# -------------------------------------------------------------
# 0. 互動式選單模式 (Interactive Menu)
# -------------------------------------------------------------
if ($Interactive) {
    Clear-Host
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host "       日常流水帳系統 (Daily Ledger System) - 啟動選單        " -ForegroundColor White
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host " 1. [預設] H2 記憶體模式 (Port 8080, 零配置即開即用)" -ForegroundColor Yellow
    Write-Host " 2. MS SQL Server 模式 (Port 8080, 連線 localhost:1433)" -ForegroundColor White
    Write-Host " 3. H2 模式 + 自動開啟瀏覽器" -ForegroundColor White
    Write-Host " 4. MS SQL 模式 + 自動開啟瀏覽器" -ForegroundColor White
    Write-Host " 5. 乾淨重構並啟動 (Maven Clean + Run)" -ForegroundColor White
    Write-Host " 6. 開啟 JVM Remote Debug 模式 (Port 5005)" -ForegroundColor White
    Write-Host " 0. 退出" -ForegroundColor DarkGray
    Write-Host "---------------------------------------------------------------" -ForegroundColor Gray
    $choice = Read-Host " 請輸入選項編號 [預設 1]"

    switch ($choice.Trim()) {
        '2' { $Profile = 'mssql' }
        '3' { $Profile = 'h2'; $OpenBrowser = $true }
        '4' { $Profile = 'mssql'; $OpenBrowser = $true }
        '5' { $Clean = $true }
        '6' { $DebugMode = $true }
        '0' { Write-Host "已取消啟動。"; exit 0 }
        default { $Profile = 'h2' }
    }
}

# -------------------------------------------------------------
# 1. 智慧前置診斷檢查 (Preflight Diagnostics)
# -------------------------------------------------------------
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = (Get-Location).Path }
$mvnwCmd = Join-Path $scriptRoot "mvnw.cmd"

if (-not (Test-Path $mvnwCmd)) {
    Write-Err "找不到 Maven Wrapper 執行檔: $mvnwCmd"
    exit 1
}

if (-not $SkipCheck) {
    Write-Header "前置環境診斷檢查"

    # 1.1 Java 版本檢測
    try {
        $javaVersionOutput = & java -version 2>&1 | Out-String
        if ($javaVersionOutput -match 'version "(\d+)') {
            $majorVer = [int]$matches[1]
            if ($majorVer -lt 21) {
                Write-Warn "偵測到 Java 主版本: $majorVer (建議使用 Java 21 或更新版本以符合 Spring Boot 3.3.x 要求)"
            } else {
                Write-Success "Java 執行環境檢查通過 (Java $majorVer)"
            }
        } else {
            Write-Info "已偵測到 Java 執行環境"
        }
    } catch {
        Write-Warn "無法執行 'java -version'，請確認系統 PATH 環境變數已正確設定 JDK 21+"
    }

    # 1.2 HTTP Port 衝突檢測
    try {
        $occupied = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($occupied) {
            $processName = (Get-Process -Id $occupied.OwningProcess -ErrorAction SilentlyContinue).ProcessName
            Write-Warn "通訊埠 $Port 目前已被 PID $($occupied.OwningProcess) ($processName) 佔用！"
            Write-Warn "若 Spring Boot 啟動失敗，請考慮使用參數指定其他通訊埠 (例如: -Port $($Port + 1))"
        } else {
            Write-Success "通訊埠 $Port 閒置可用"
        }
    } catch {
        # 若非管理員或舊版 PowerShell 則跳過
    }

    # 1.3 MS SQL Server TCP 1433 連線探測
    if ($Profile -eq 'mssql') {
        Write-Info "正在探測 MS SQL Server (localhost:1433) 連線..."
        try {
            $tcp = Test-NetConnection -ComputerName "localhost" -Port 1433 -WarningAction SilentlyContinue
            if ($tcp.TcpTestSucceeded) {
                Write-Success "MS SQL Server (localhost:1433) 連線通暢"
            } else {
                Write-Warn "無法連線至 MS SQL Server (localhost:1433)！"
                Write-Warn "請確認本機 SQL Server 服務已啟動，並已開啟 TCP/IP 協定 (預設 Port 1433)。"
            }
        } catch {
            Write-Info "無法執行網路連線測試，將由 Spring Boot 自行嘗試連線。"
        }
    }
}

# -------------------------------------------------------------
# 2. 執行 Maven Clean (若指定)
# -------------------------------------------------------------
if ($Clean) {
    Write-Header "執行 Maven Clean"
    & $mvnwCmd clean
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Maven Clean 失敗，停止啟動。"
        exit $LASTEXITCODE
    }
    Write-Success "Maven Clean 完成"
}

# -------------------------------------------------------------
# 3. 組合 Spring Boot 啟動參數
# -------------------------------------------------------------
$mvnArgs = @("spring-boot:run")

# Profile 設定 (h2 使用預設 application.yml, mssql 使用 application-mssql.yml)
if ($Profile -ne 'h2') {
    $mvnArgs += "-Dspring-boot.run.profiles=$Profile"
}

# 傳遞 Server Port
$springArgs = @()
if ($Port -ne 8080) {
    $springArgs += "--server.port=$Port"
}

if ($springArgs.Count -gt 0) {
    $mvnArgs += "-Dspring-boot.run.arguments=$($springArgs -join ' ')"
}

# JVM Remote Debugger
if ($DebugMode) {
    $mvnArgs += '-Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"'
    Write-Info "已開啟 JVM 遠端除錯 (Listening on port 5005)"
}

# 透傳額外參數
if ($ExtraArgs) {
    $mvnArgs += $ExtraArgs
}

# -------------------------------------------------------------
# 4. 背景非同步就緒檢測與自動開啟瀏覽器 (若指定)
# -------------------------------------------------------------
$targetUrl = "http://localhost:$Port/"
if ($OpenBrowser) {
    Write-Info "已註冊瀏覽器自動開啟作業，伺服器啟動完成後將自動前往 $targetUrl"
    Start-Job -ScriptBlock {
        param($url, $checkPort)
        $maxAttempts = 60
        $attempt = 0
        while ($attempt -lt $maxAttempts) {
            Start-Sleep -Seconds 1
            $attempt++
            try {
                $client = New-Object System.Net.Sockets.TcpClient
                $connect = $client.BeginConnect("localhost", $checkPort, $null, $null)
                $success = $connect.AsyncWaitHandle.WaitOne(500, $false)
                if ($success -and $client.Connected) {
                    $client.EndConnect($connect)
                    $client.Close()
                    Start-Sleep -Milliseconds 800
                    Start-Process $url
                    break
                }
                $client.Close()
            } catch {
                # 尚未就緒，繼續等待
            }
        }
    } -ArgumentList $targetUrl, $Port | Out-Null
}

# -------------------------------------------------------------
# 5. Swiss Style 啟動看板與端點清單
# -------------------------------------------------------------
$dbTitle = if ($Profile -eq 'mssql') { "MS SQL Server (localhost:1433/tibame_account)" } else { "H2 In-Memory (jdbc:h2:mem:tibame_account)" }
$h2ConsoleUrl = if ($Profile -eq 'h2') { "http://localhost:$Port/h2-console" } else { "N/A (MSSQL 模式)" }

Write-Host ""
Write-Host "+-----------------------------------------------------------------------------+" -ForegroundColor DarkCyan
Write-Host "|           TIBAME DAILY LEDGER SYSTEM - SWISS EDITION 2026                   |" -ForegroundColor Cyan
Write-Host "+-----------------------------------------------------------------------------+" -ForegroundColor DarkCyan
Write-Host "  模式 (Profile) : " -NoNewline -ForegroundColor Gray; Write-Host "$Profile" -ForegroundColor Yellow
Write-Host "  資料庫 (DB)    : " -NoNewline -ForegroundColor Gray; Write-Host "$dbTitle" -ForegroundColor White
Write-Host "  通訊埠 (Port)  : " -NoNewline -ForegroundColor Gray; Write-Host "$Port" -ForegroundColor White
Write-Host "  系統首頁網址   : " -NoNewline -ForegroundColor Gray; Write-Host "$targetUrl" -ForegroundColor Green
Write-Host "  H2 管理後台    : " -NoNewline -ForegroundColor Gray; Write-Host "$h2ConsoleUrl" -ForegroundColor Cyan
Write-Host "  預設測試帳號   : " -NoNewline -ForegroundColor Gray; Write-Host "admin / 123456  (一般帳號: user / 123456)" -ForegroundColor DarkYellow
Write-Host "+-----------------------------------------------------------------------------+" -ForegroundColor DarkCyan
Write-Host "  正在透過 Maven Wrapper 啟動 Spring Boot 服務... (按 Ctrl+C 可停止服務)`n" -ForegroundColor DarkGray

# -------------------------------------------------------------
# 6. 啟動 Spring Boot
# -------------------------------------------------------------
& $mvnwCmd @mvnArgs
