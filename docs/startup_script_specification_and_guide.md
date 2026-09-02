# 日常流水帳系統 (Daily Ledger System) — 啟動腳本 (start.ps1) 規格與維運指南

本文件定義「日常流水帳系統 (Daily Ledger System)」之本機啟動與維運管理腳本（`start.ps1`）的完整設計規格、參數定義、智慧診斷機制與操作手冊。

---

## 1. 概述與設計目標 (Overview & Objectives)

為提升開發效率並降低跨環境部署與測試的門檻，專案根目錄提供標準化的 PowerShell 啟動腳本 `start.ps1`。

### 核心目標
1. **開箱即用 (Zero-Configuration)**：預設以 H2 記憶體資料庫啟動，無須預先安裝與設定 MS SQL Server 即可一鍵運行。
2. **無縫雙模式切換 (Dual-Database Profiles)**：支援一鍵切換 `h2` (記憶體/開發測試) 與 `mssql` (MS SQL Server/正式體驗) 設定檔。
3. **智慧前置診斷 (Preflight Diagnostics)**：在啟動 Spring Boot 之前，自動檢查 Java 21 版本、Port 衝突（提示佔用 PID 與程序名稱）、SQL Server 1433 連線狀態。
4. **自動化體驗 (Automation & UX)**：支援伺服器就緒後自動開啟預設瀏覽器、遠端 JVM 除錯埠（5005）開啟、互動式選單及任意參數透傳。

---

## 2. 系統架構與執行流程 (Execution Flow & Architecture)

```mermaid
graph TD
    Start(["執行 .\start.ps1 [參數]"]) --> ParseParams["解析命令列參數與別名"]
    ParseParams --> CheckInteractive{"是否指定 -Interactive (-i)?"}
    CheckInteractive -->|是| ShowMenu["顯示互動式數字選單並取得使用者輸入"]
    CheckInteractive -->|否| CheckSkip{"是否指定 -SkipCheck?"}
    ShowMenu --> CheckSkip

    CheckSkip -->|否 (預設)| Preflight["前置診斷檢查 (Preflight Checks)"]
    CheckSkip -->|是| BuildArgs["略過檢查，直接組裝參數"]

    subgraph Diagnostics ["智慧診斷機制"]
        Preflight --> CheckJava["1. 檢查 Java 版本 >= 21"]
        CheckJava --> CheckPort["2. 檢查 HTTP Port 是否被佔用 (如 8080)"]
        CheckPort --> CheckMSSQL{"3. 是否為 MSSQL Profile?"}
        CheckMSSQL -->|是| PingSQL["測試 localhost:1433 TCP 連線"]
        CheckMSSQL -->|否| BuildArgs
        PingSQL --> BuildArgs
    end

    BuildArgs --> CleanCheck{"是否指定 -Clean (-c)?"}
    CleanCheck -->|是| ExecClean["執行 .\mvnw.cmd clean"]
    CleanCheck -->|否| ShowBanner["印出 Swiss Style 啟動資訊看板"]
    ExecClean --> ShowBanner

    ShowBanner --> BrowserWatcher{"是否指定 -OpenBrowser (-b)?"}
    BrowserWatcher -->|是| StartWatcher["啟動背景非同步 Job 監測 HTTP 端點就緒並開啟瀏覽器"]
    BrowserWatcher -->|否| RunSpringBoot["執行 .\mvnw.cmd spring-boot:run"]
    StartWatcher --> RunSpringBoot

    RunSpringBoot --> Terminate(["服務運行中 (Ctrl+C 終止)"])
```

---

## 3. 參數定義與規格 (Parameter Specifications)

`start.ps1` 採用 PowerShell 5.1 / 7+ 之 `[CmdletBinding()]` 標準宣告，支援完整的參數驗證、型態轉換與別名：

| 參數名稱 | 別名 | 型態 | 預設值 | 說明與範例 |
| :--- | :--- | :--- | :--- | :--- |
| `-Profile` | `-p`, `-Mode` | `String` (ValidateSet: `h2`, `mssql`, `prod`) | `'h2'` | 指定 Spring Active Profile。<br>• `h2`: 記憶體資料庫 (`application.yml`)<br>• `mssql`: 本機 MS SQL Server (`application-mssql.yml`) |
| `-Port` | `-serverPort`, `-httpPort` | `Int32` | `8080` | 指定 Spring Boot 伺服器監聽之 HTTP Port。 |
| `-Clean` | `-c` | `Switch` | `$false` | 啟動前是否先執行 Maven Clean 清除建置快取。 |
| `-DebugMode` | `-d` | `Switch` | `$false` | 開啟 JVM Remote Debug 監聽埠（`address=*:5005`），供 IDE 附加除錯。 |
| `-OpenBrowser` | `-b` | `Switch` | `$false` | 啟動非同步監視器，當後端服務就緒後自動開啟系統首頁。 |
| `-Interactive` | `-i` | `Switch` | `$false` | 開啟互動式終端選單，可按數字鍵選擇常用啟動模式。 |
| `-SkipCheck` |  | `Switch` | `$false` | 略過 Java 版本、Port 佔用與資料庫連線等所有前置檢查。 |
| `$ExtraArgs` |  | `String[]` | `$null` | 透傳額外參數至 Maven 或 Spring Boot（以 `--` 傳遞）。 |

---

## 4. 智慧檢查與診斷機制 (Preflight Diagnostics)

### 4.1 Java 21 版本驗證
- **邏輯**：調用 `java -version` 並解析主版本號。
- **規則**：若未偵測到 Java 或版本小於 21，將以黃色警示提示使用者，避免 Spring Boot 3.3.x 啟動時發生 `UnsupportedClassVersionError`。

### 4.2 通訊埠衝突檢測 (Port Conflict Detection)
- **邏輯**：使用 `Get-NetTCPConnection` 檢測目標 Port（如 `8080`）。
- **回饋**：若已被佔用，自動解析該連線的 `OwningProcess`，查詢其程序名稱（Process Name）與 PID 並顯示警告，防止啟動時發生 `PortInUseException`。

### 4.3 MS SQL Server TCP 1433 連線探測
- **邏輯**：當 `-Profile mssql` 時，透過 `Test-NetConnection -ComputerName localhost -Port 1433` 進行 Socket 測試。
- **回饋**：若未能連通，輸出明確警告：「*無法連線至 MS SQL Server (localhost:1433)，請確認本機 SQL Server (MSSQLSERVER / SQLEXPRESS) 服務已啟動。*」

---

## 5. 終端機視覺設計 (Swiss Style CLI UX)

遵循瑞士風格（Swiss Design Style）的簡潔、結構化與高對比原則，啟動時輸出清晰的狀態看板：

```text
+-------------------------------------------------------------+
|    TIBAME DAILY LEDGER SYSTEM — SWISS EDITION 2026          |
+-------------------------------------------------------------+
 [狀態] 模式: H2 (In-Memory) | 埠號: 8080 | Java: 21.0.x
 [端點] 系統首頁:   http://localhost:8080/
 [端點] H2 Console: http://localhost:8080/h2-console
                    (JDBC URL: jdbc:h2:mem:tibame_account / sa / '')
 [端點] API 規格:   http://localhost:8080/api/v1/...
+-------------------------------------------------------------+
```

---

## 6. 使用場景與指令速查表 (Cheatsheet)

### 常用指令

```powershell
# 1. 預設啟動 (H2 模式, 8080 Port, 零設定開箱即用)
.\start.ps1

# 2. 使用 MS SQL Server 啟動
.\start.ps1 -Profile mssql
# 簡寫:
.\start.ps1 -p mssql

# 3. 指定通訊埠 9090 並在就緒後自動打開瀏覽器
.\start.ps1 -Port 9090 -OpenBrowser
# 簡寫:
.\start.ps1 -P 9090 -b

# 4. 乾淨重構並啟動 (Clean + Run)
.\start.ps1 -Clean

# 5. 開啟 JVM Remote Debugger (Port 5005) 供 VS Code / IDEA 除錯
.\start.ps1 -DebugMode

# 6. 互動式選單模式 (新手推薦)
.\start.ps1 -Interactive

# 7. 透傳 Spring Boot 參數 (例如開啟 SQL 日誌或自訂屬性)
.\start.ps1 -- --logging.level.org.hibernate.SQL=DEBUG
```

---

## 7. 維護與故障排除 (Troubleshooting)

| 異常情境 | 可能原因 | 建議處置 |
| :--- | :--- | :--- |
| `java: command not found` | 系統環境變數未加入 JDK `bin` 路徑 | 安裝 JDK 21 並將 `JAVA_HOME` 與 `PATH` 設定完成 |
| `Port 8080 is already in use` | 其他應用程式或先前的 Java 程序未關閉 | 使用 `start.ps1` 提示的 PID 執行 `Stop-Process -Id <PID>`，或使用 `-Port 8081` |
| `MSSQL 連線失敗 (TCP 1433)` | MS SQL Server 服務未啟動或 TCP/IP 協定未啟用 | 在 Windows 服務中啟動 `SQL Server` 服務，並至 SQL Server 組態管理員確認 TCP/IP 已啟用 |
