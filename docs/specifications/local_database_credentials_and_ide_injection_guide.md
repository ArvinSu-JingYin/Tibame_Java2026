# 日常流水帳系統 — 本機資料庫密碼零改動注入與 IDE 隔離機制規格手冊 (Local Database Credentials Zero-Code IDE Injection & Security Guide)

> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **系統名稱**：日常流水帳系統 (Daily Ledger System)  
> **資安規範**：OWASP ASVS / 雲原生 12-Factor App III. Config 原則 / 本地開發憑證隔離標準  
> **核心政策**：原始碼零改動 (Zero Code Changes) & 工作區零污染 (Clean Git Working Tree) & CI/CD 絕對隔離 (Complete CI Pipeline Isolation)  
> **關聯探索**：[本機資料庫密碼零改動注入與 IDE 隔離機制探索報告 (local_database_credentials_and_ide_injection_exploration.md)](../explorations/local_database_credentials_and_ide_injection_exploration.md)  
> **正式環境手冊**：[正式伺服器環境變數安全注入與部署維運規格手冊 (production_deployment_and_environment_variables_security_guide.md)](production_deployment_and_environment_variables_security_guide.md)  

---

## 1. 概述與四大核心原則 (Overview & 4 Core Principles)

在微軟 SQL Server（MS SQL）的日常開發過程中，不同工程師的本機環境通常具有不同的安全配置。例如部分工程師本機的 SQL Server 實例密碼為個人專用密碼（非預設弱密碼 `1111`）。

若直接在被 Git 追蹤的設定檔（如 `src/main/resources/application-mssql.yml`）中修改密碼，將引發機密外洩風險、Git 工作區污染（Dirty Tree）、Git 提交歷史不可逆洩露以及協作合併衝突等痛點。

日常流水帳系統全面規範「IDE 行程環境變數動態注入」與「Git 嚴格隔離」機制，落實四大工程治理原則：

```
+-------------------------------------------------------------------------------+
|                             本機機密注入四大核心原則                          |
+-------------------------------------------------------------------------------+
|                                                                               |
|  1. 原始碼零改動 (Zero Code Changes):                                         |
|     * 嚴禁修改 src/main/resources/ 底下任何被 Git 追蹤的 YAML / 程式檔。       |
|                                                                               |
|  2. 工作區零污染 (Clean Git Working Tree):                                    |
|     * git status 永遠維持 100% 乾淨，絕無任何個人連線憑證等待提交。          |
|                                                                               |
|  3. 開箱即用與平滑切換 (Zero Friction & Fast Switch):                         |
|     * 支援一鍵 F5 無感啟動本機自訂 MSSQL，亦可隨時切回預設 H2 記憶體模式。    |
|                                                                               |
|  4. CI/CD 絕對隔離 (Complete CI Pipeline Isolation):                          |
|     * 本地的一切機密注入設定，對 GitHub Actions 遠端建置產生 100% 零干擾。    |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. Spring Boot 外部化配置機制與優先級階層 (Spring Boot Hierarchy)

Spring Boot 原生具備強大的階層式外部化配置能力（Externalized Configuration）。在專案中的 [application-mssql.yml](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/resources/application-mssql.yml) 設定如下：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=tibame_account;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true;}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:1111}
```

語法 `${DB_PASSWORD:1111}` 代表：**優先讀取外部注入之環境變數 `DB_PASSWORD`，若未提供，則備用回退（Fallback）為 `1111`**。

### 2.1 外部化屬性覆蓋優先順序 (Dataflow)

```
+-------------------------------------------------------------------------------+
|                 Spring Boot 密碼解析優先順序 (優先度由高至低)                 |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [優先級 1] 命令列參數 (Command Line Arguments):                              |
|             --spring.datasource.password=MySecretPass                         |
|      ^                                                                        |
|      | 覆蓋 (Override)                                                        |
|                                                                               |
|  [優先級 2] 行程環境變數 (OS / IDE Injected Environment Variables):           |
|             DB_PASSWORD=MySecretPass                                          |
|      ^                                                                        |
|      | 覆蓋 (Override)                                                        |
|                                                                               |
|  [優先級 3] 專案外部設定檔 (External Config File, 如 ./config/application.yml) |
|      ^                                                                        |
|      | 覆蓋 (Override)                                                        |
|                                                                               |
|  [優先級 4] 專案內 Classpath 預設值 (application-mssql.yml 內嵌備案):         |
|             1111 (僅供零設定快速體驗)                                         |
|                                                                               |
+-------------------------------------------------------------------------------+
```

由上述機制可知：**只要在優先級 2 注入 JVM 行程環境變數 `DB_PASSWORD`，即可完全覆蓋 Classpath 預設值，無須動到任何專案原始碼或設定檔！**

---

## 3. IDE 啟動組態範本與雙軌設計 (.vscode/launch.json)

專案在 `.vscode/` 目錄下提供已納入版本控管的標準啟動範本 [.vscode/launch.json.example](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.vscode/launch.json.example)：

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot: 本機 MSSQL (含自訂密碼)",
      "request": "launch",
      "mainClass": "com.tibame.DailyLedgerApplication",
      "projectName": "daily-ledger-system",
      "args": "--spring.profiles.active=mssql",
      "env": {
        "DB_PASSWORD": "請替換為你的本機MSSQL真實密碼",
        "DB_USERNAME": "sa"
      }
    },
    {
      "type": "java",
      "name": "Spring Boot: H2 記憶體快速測試模式 (預設)",
      "request": "launch",
      "mainClass": "com.tibame.DailyLedgerApplication",
      "projectName": "daily-ledger-system"
    }
  ]
}
```

### 3.1 雙軌模式特點說明

1. **軌道 1：Spring Boot: 本機 MSSQL (含自訂密碼)**：
   - 啟動參數指定 `--spring.profiles.active=mssql`。
   - 藉由 `env` 區塊注入 `DB_PASSWORD` 與 `DB_USERNAME`，僅存活於當前 Java 啟動行程中。
   - 適用情境：需要對接本機 MS SQL 進行真實 SQL 實體測試、預存程序偵錯或資料持久化檢核。
2. **軌道 2：Spring Boot: H2 記憶體快速測試模式 (預設)**：
   - 不指定外部 profile 與自訂密碼，開箱直接使用預設內嵌 H2 記憶體資料庫。
   - 適用情境：快速驗證 Web Controller、前端頁面樣式調整、無 MS SQL 服務之輕量開發環境。

---

## 4. 開發者標準作業程序 (Developer SOP)

### 4.1 第一次建立個人啟動組態 (Step-by-Step)

1. **複製範本檔案**：
   在 VS Code / Antigravity IDE 終端機中執行：
   ```powershell
   Copy-Item .vscode/launch.json.example .vscode/launch.json
   ```
2. **修改個人密碼**：
   開啟 `.vscode/launch.json`，將 `"請替換為你的本機MSSQL真實密碼"` 替換為個人本機 MS SQL Server 的真實密碼並存檔。
3. **驗證 Git 乾淨度**：
   執行 `git status`，確認 `.vscode/launch.json` 未出現在待提交檔案清單中（已被 `.gitignore` 阻斷）。

### 4.2 日常偵錯與模式切換操作

1. 點擊 IDE 左側活動列的 **「執行與偵錯 (Run and Debug)」** 圖示（或鍵盤快捷鍵 `Ctrl + Shift + D`）。
2. 在頂端執行組態下拉選單中：
   - 選擇 **`Spring Boot: 本機 MSSQL (含自訂密碼)`** 即可連線本機 MS SQL。
   - 選擇 **`Spring Boot: H2 記憶體快速測試模式 (預設)`** 即可回退至乾淨 H2 記憶體資料庫。
3. 按下鍵盤 **`F5`** 鍵即可以偵錯模式啟動（支援中斷點、呼叫堆疊檢視與變數監看）；按 **`Ctrl + F5`** 則可無中斷點快速運行。

---

## 5. 版本控制防護與 .gitignore 白名單機制 (VCS Guard)

為兼顧「團隊享有統一範本」與「個人機密絕對不入版控」，專案 [.gitignore](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.gitignore) 採用目錄萬用排除搭配範本白名單語法：

```gitignore
### ===================================================================
### IDE: Visual Studio Code & Editors (Single Developer)
### ===================================================================
.vscode/*
!.vscode/*.example
*.code-workspace
.history/
*~
*.swp
*.swo
```

### 5.1 防護機制驗證命令

可透過 Git 原生 `check-ignore` 指令驗證阻絕效果：

```bash
# 驗證 1：含個人密碼之 launch.json 必須被忽略
git check-ignore -v .vscode/launch.json
# 預期輸出: .gitignore:83:.vscode/*    .vscode/launch.json

# 驗證 2：共用範本檔 launch.json.example 必須可受版控追蹤
git check-ignore -v .vscode/launch.json.example
# 預期輸出: .gitignore:84:!.vscode/*.example    .vscode/launch.json.example
```

---

## 6. CI/CD 與 GitHub Actions 隔離保證 (CI/CD Decoupling)

本機所有 IDE 環境變數與 `launch.json` 設定對 GitHub Actions CI 管線具有 **100% 絕對隔離性**：

```
+-------------------------------------------------------------------------------+
|                      本機開發 vs GitHub Actions CI 絕對隔離圖                 |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [ 本機開發端 (Local Workstation) ]                                           |
|  +-------------------------------------------------------+                    |
|  |  VS Code / Antigravity                                |                    |
|  |  [ .vscode/launch.json (本機專屬，受 .gitignore 阻絕) ] |                    |
|  |  注入變數: DB_PASSWORD="MyPersonalSecret"             |                    |
|  |  啟動參數: --spring.profiles.active=mssql             |                    |
|  +---------------------------+---------------------------+                    |
|                              | (僅限本地 JVM 行程記憶體)                      |
|                              v                                                |
|  +-------------------------------------------------------+      Git Push      |
|  |  Spring Boot 應用程式本機行程                         |  ----------------> |
|  |  - 成功取得個人密碼並連線本機 MS SQL 實例              |         X          |
|  +-------------------------------------------------------+   .gitignore 阻斷  |
|                                                              (絕不上傳 GitHub)|
| ----------------------------------------------------------------------------- |
|                                                                               |
|  [ 遠端 CI 管線 (GitHub Actions Cloud Runner) ]                               |
|  +-------------------------------------------------------+                    |
|  |  觸發事件: Push / Pull Request (Ubuntu-Latest 虛擬機)  |                    |
|  +---------------------------+---------------------------+                    |
|                              |                                                |
|                              v                                                |
|  +-------------------------------------------------------+                    |
|  |  執行指令: ./mvnw clean test / clean verify           |                    |
|  |  - 環境特性: 乾淨無狀態 (Stateless Ephemeral VM)       |                    |
|  |  - 啟動 Profile: 預設 (Default，無 mssql 參數)        |                    |
|  |  - 資料庫實體: H2 In-Memory Database                  |                    |
|  |  - 連線密碼: 空字串 '' (原生無需任何密碼)             |                    |
|  |  - 執行結果: 100% 綠燈通過 (與本地 IDE 設定毫無牽涉)   |                    |
|  +-------------------------------------------------------+                    |
|                                                                               |
+-------------------------------------------------------------------------------+
```

1. **版本控制防護層 (VCS Barrier)**：`.vscode/launch.json` 根本無法推送到遠端儲存庫。
2. **管線執行隔離層 (Execution Barrier)**：CI 流程 `ci-pr.yml` 與 `ci-main.yml` 以 Maven 原生無狀態模式運行，不載入任何 IDE 擴充套件。
3. **資料庫引擎隔離層 (Database Engine Barrier)**：CI 建置預設啟用內嵌 H2 資料庫，不需要 MS SQL 亦不需要本機密碼。

---

## 7. 常見問題與除錯手冊 (FAQ & Troubleshooting)

### Q1：如果不透過 IDE，如何在 PowerShell 終端機手動啟動本機 MSSQL 模式？
若需在終端機執行 `mvn spring-boot:run`，可利用 PowerShell 臨時行程環境變數啟動，關閉該視窗後變數即自動銷毀：
```powershell
$env:DB_PASSWORD="你的本機SQL真實密碼"
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=mssql"
```

### Q2：若不小心在範本檔 `.vscode/launch.json.example` 中填寫了真實密碼怎麼辦？
1. 請立即將 `.vscode/launch.json.example` 復原為預設佔位符 `"請替換為你的本機MSSQL真實密碼"`。
2. 檢查 `git status` 與 `git diff`，確認範本檔內容乾淨。
3. 確保真實密碼僅填寫在未受版控的 `.vscode/launch.json` 中。
