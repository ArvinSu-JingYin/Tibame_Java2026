# 本機資料庫密碼零改動注入與 IDE 隔離機制探索報告 (Local Database Credentials Zero-Code IDE Injection Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Spring Boot 3.3.13 / VS Code / Antigravity IDE / GitHub Actions / MS SQL Server / Git  
> **目標範疇**：本機開發私密憑證管理、原始碼零改動、Git 乾淨度治理、CI/CD 完全隔離  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **關聯文件**：[資料庫連線憑證與 JWT 簽署金鑰安全管理策略探索報告 (database_credentials_and_jwt_secret_security_exploration.md)](database_credentials_and_jwt_secret_security_exploration.md) / [正式伺服器環境變數安全注入與部署維運規格手冊 (production_deployment_and_environment_variables_security_guide.md)](../specifications/production_deployment_and_environment_variables_security_guide.md)

---

## 1. 探索背景與本機開發痛點 (Background & Local Dev Pain Points)

在微軟 SQL Server（MS SQL）的日常開發過程中，不同工程師的本機環境通常具有不同的安全配置。例如部分工程師本機的 SQL Server 實例密碼為個人設定的專用密碼（非預設弱密碼 `1111`）。

若直接在被 Git 追蹤的設定檔中修改密碼，將引發嚴重的工程與資安痛點：

### 1.1 核心痛點剖析 (Pain Points Analysis)

1. **Git Commit 污染與誤推送風險 (Accidental Push)**：
   - 一旦在 `src/main/resources/application-mssql.yml` 修改了本機密碼，該檔案即處於 Dirty 狀態。
   - 工程師在提交業務代碼時，容易誤將包含個人密碼的 YAML 檔案一併 `git add` 並 Push 到 GitHub，造成機密外洩。
2. **Git 歷史紀錄的不可逆性 (Git History Immutability)**：
   - 即使事後發現並在下一次 Commit 覆蓋或還原，**舊的 Commit 紀錄中依然永遠留存該明文密碼**，必須透過破壞性指令（如 `git-filter-repo`）重寫歷史才能徹底抹除。
3. **協作衝突 (Collaboration Friction)**：
   - 每位團隊成員本地的密碼若不同，每個人修改 `application-mssql.yml` 會造成頻繁的 Git Merge 衝突。
4. **CI 流程疑慮 (CI/CD Pipeline Concerns)**：
   - 工程師常擔心本機若自訂環境變數或啟動參數，是否會干擾 GitHub Actions CI 自動化建置與測試。

### 1.2 核心目標 (Core Objectives)

```
+-------------------------------------------------------------------------------+
|                             本機機密注入四大核心原則                          |
+-------------------------------------------------------------------------------+
|                                                                               |
|  1. 原始碼零改動 (Zero Code Changes):                                         |
|     * 嚴禁修改 src/main/resources/ 底下任何被 Git 追蹤的 YAML / 程式檔。       |
|                                                                               |
|  2. 工作區零污染 (Clean Git Working Tree):                                    |
|     * git status 永遠維持乾淨，絕無任何機密連線參數等待提交。                |
|                                                                               |
|  3. 開箱即用與平滑切換 (Zero Friction & Fast Switch):                         |
|     * 支援一鍵無感啟動本機自訂 MSSQL，亦可隨時切回預設 H2 記憶體模式。        |
|                                                                               |
|  4. CI/CD 絕對隔離 (Complete CI Pipeline Isolation):                          |
|     * 本地的一切機密注入設定，對 GitHub Actions 遠端建置產生 100% 零影響。     |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. Spring Boot 組態屬性優先順序解析 (Spring Boot Configuration Hierarchy)

Spring Boot 原生採用了階層式外部化配置機制（Externalized Configuration）。在專案中的 [application-mssql.yml](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/resources/application-mssql.yml) 設定如下：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=tibame_account;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true;}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:1111}
```

這項語法 `${DB_PASSWORD:1111}` 代表：**優先讀取外部注入的 `DB_PASSWORD`，若未提供，則備用回退（Fallback）為 `1111`**。

### 2.1 優先級階層流向圖 (Hierarchy Dataflow)

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

由上述機制可知：**只要在優先級 1 或 2 注入 `DB_PASSWORD`，即可完全凌駕 Classpath 預設值，無須動到任何專案檔案！**

---

## 3. 四大本地機密管理途徑深度對比 (Evaluation of Approaches)

```
+-------------------------------------------------------------------------------------------------------------------+
|                                            四大本機機密注入方案對比矩陣                                           |
+-------------------+------------------+------------------+------------------+------------------+-------------------+
| 評估維度          | 途徑 A: IDE 組態 | 途徑 B: 系統變數 | 途徑 C: 外掛 YAML| 途徑 D: 啟動腳本 | 推薦指數          |
+-------------------+------------------+------------------+------------------+------------------+-------------------+
| Git 工作區乾淨度  | 100% 乾淨 (排除) | 100% 乾淨 (無檔) | 需注意排除規則   | 需額外排除腳本   | 途徑 A / B 最佳   |
| 專案環境隔離性    | 完全隔離         | 全電腦所有專案共用| 完全隔離         | 完全隔離         | 途徑 A / C / D 佳 |
| 開發體驗 (DX)     | 一鍵點擊 F5 啟動 | 任意方式皆可啟動 | 需手動維護檔案   | 需透過特定腳本   | 途徑 A / B 佳     |
| 終端機 (CLI) 相容 | 限 IDE 內執行    | 全環境 Shell 通用| 支援 CLI         | 專屬 Shell 支援  | 途徑 B 佳         |
| CI 流程干擾風險   | 0% (絕對隔離)    | 0% (絕對隔離)    | 0% (已忽略)      | 0% (已忽略)      | 全數安全          |
+-------------------+------------------+------------------+------------------+------------------+-------------------+
```

### 3.1 方案細節與選型結論

* **途徑 A（IDE 執行組態注入）— [最推薦]**：
  * 機制：利用 IDE 啟動時動態將環境變數寫入 Java 行程中。
  * 優勢：設定檔存在於 `.vscode/launch.json`，而專案 [.gitignore](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.gitignore#L83) 早已預先宣告 `.vscode/` 排除，既隔離又乾淨。
* **途徑 B（Windows 使用者環境變數）**：
  * 機制：在 Windows 系統控制台中新增 `DB_PASSWORD`。
  * 優勢：隨開隨跑，甚至在終端機敲 `mvn spring-boot:run` 都能自動吃。
  * 劣勢：如果電腦上有其他 Java 或 Node 專案碰巧也叫 `DB_PASSWORD`，會產生變數污染。
* **途徑 C（未追蹤的本地 YAML，如 `application-local.yml`）**：
  * 機制：在本地新增 `application-local.yml`（已受 `.gitignore` 保護），啟動時附加 `--spring.profiles.active=mssql,local`。
  * 劣勢：每次啟動參數多串一個 profile，操作手續略繁瑣。
* **途徑 D（PowerShell 輔助啟動腳本）**：
  * 機制：建立未被追蹤的 `run-local.ps1` 腳本封裝環境變數與啟動指令。

**決策**：在 VS Code / Antigravity IDE 環境下，**途徑 A 是兼顧「極致乾淨」、「單一專案隔離」與「最舒適除錯體驗」的最佳解**。

---

## 4. CI/CD 與 GitHub Actions 隔離性深度驗證 (CI Pipeline Isolation Analysis)

許多工程師最核心的疑問是：**「我在本機 IDE 設定了自訂密碼與環境變數，會不會影響 GitHub Actions CI？」**

答案是：**100% 完全零影響！兩者在實體、邏輯與設定層次徹底解耦。**

### 4.1 本機 vs 遠端 CI 雙軌架構對比圖

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

### 4.2 三大隔離保證

1. **版本控制防護層 (VCS Barrier)**：
   - 專案的 [.gitignore](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.gitignore) 明文排除了 `.vscode/`、`.idea/`、`application-local.yml` 及 `.env`。本地檔案根本無法進入 Git Tree。
2. **建置指令隔離層 (Execution Barrier)**：
   - GitHub Actions 執行檔 [ci-pr.yml](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.github/workflows/ci-pr.yml) 與 [ci-main.yml](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/.github/workflows/ci-main.yml) 僅執行純 Maven 指令（如 `./mvnw clean test`），不依賴任何 IDE 擴充套件或啟動組態。
3. **資料庫引擎隔離層 (Database Engine Barrier)**：
   - CI 管線並未配置本機 MS SQL Server，預設直接啟用 [application.yml](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/resources/application.yml) 的內嵌 H2 資料庫（`jdbc:h2:mem:tibame_account`），密碼固定為空字串 `''`，因此完全不需要外界提供任何 SQL 密碼。

---

## 5. VS Code / Antigravity 實踐操作指南 (VS Code SOP)

### 5.1 建立本地啟動組態檔 (`.vscode/launch.json`)

在專案根目錄建立 `.vscode/launch.json`（已在 `.gitignore` 名單中）：

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
        "DB_PASSWORD": "你的本機SQL真實密碼",
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

### 5.2 日常操作步驟

1. **填寫密碼**：將上述設定中的 `"你的本機SQL真實密碼"` 改為本機 SQL Server 的真實密碼並存檔。
2. **開啟執行面板**：點擊 VS Code / Antigravity 左側活動列的 **「執行與偵錯 (Run and Debug)」** 圖示（或按快捷鍵 `Ctrl + Shift + D`）。
3. **選擇啟動組態**：在頂端下拉選單中選擇 **`Spring Boot: 本機 MSSQL (含自訂密碼)`**。
4. **啟動或偵錯**：
   - 按 `F5`（啟動偵錯模式，支援中斷點與變數監看）。
   - 按 `Ctrl + F5`（無偵錯模式直接運行）。
5. **切換模式**：若想進行輕量測試或不連 SQL Server，只需在下拉選單切換至 **`Spring Boot: H2 記憶體快速測試模式 (預設)`** 即可。

---

## 6. 總結與治理建議 (Summary & Governance Recommendations)

```
+-------------------------------------------------------------------------------+
|                            本機開發資安最佳實踐清單                           |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [V] 嚴守「原始碼零機密」原則，不將任何真實密碼寫死在 application*.yml 中。     |
|  [V] 充分利用 ${DB_PASSWORD:1111} 語法，兼顧新進成員開箱即用與老手自訂彈性。   |
|  [V] 善用 .vscode/launch.json 注入個人環境變數，保持 Git Working Tree 乾淨。  |
|  [V] 確認 .gitignore 確實包含 .vscode/ 與本地設定檔，嚴防誤加。               |
|  [V] 理解 CI 管線與本機 IDE 完全獨立，放心在本機進行各項個人化設定。          |
|                                                                               |
+-------------------------------------------------------------------------------+
```
