## Context

本設計根植於 `docs/explorations/local_database_credentials_and_ide_injection_exploration.md` 的探索成果。當前 Spring Boot 專案在 `application-mssql.yml` 中配置了 `${DB_PASSWORD:1111}` 與 `${DB_USERNAME:sa}`。
依據 Spring Boot 外部化配置機制（Externalized Configuration Hierarchy），JVM 行程環境變數（Environment Variables）之優先順序高於 Classpath 下的 YAML 檔案。
因此，透過 IDE 在啟動 JVM 時動態注入 `DB_PASSWORD`，可達成「原始碼與設定檔零修改」即可連接個人本機 MSSQL 實例。

## Goals / Non-Goals

**Goals:**
- 提供開箱即用的 IDE 啟動範本 `.vscode/launch.json.example`，預設提供「本機 MSSQL」與「H2 記憶體」兩種執行組態。
- 配置與驗證 `.gitignore`，確保包含真實機密的 `.vscode/launch.json` 100% 阻絕於版本控制之外，同時允許範本檔受 Git 追蹤。
- 確立本機配置與 GitHub Actions CI 之實體與邏輯隔離，保證遠端自動化建置不受本地個人密碼干擾。
- 建立標準化開發人員指引文件，提供無痛上手與切換步驟。

**Non-Goals:**
- 不引入外部密鑰管理伺服器（如 HashiCorp Vault），避免增加單機開發之環境複雜度。
- 不修改 Spring Boot 既有資料庫連線核心代碼與測試結構。
- 不強制作業系統層級之全域環境變數設定，防止跨專案變數名稱污染。

## Decisions

### 決策 1：採用 IDE 行程層級環境變數注入 (`launch.json`) 為標準方案
- **選型理由**：
  - **專案環境隔離**：變數僅存在於當前專案被啟動的 JVM 行程記憶體中，關閉即銷毀，不影響作業系統與其他專案。
  - **開發體驗極佳**：工程師在 VS Code / Antigravity 中直接按 `F5` 即可啟動偵錯、設定中斷點與監看變數。
  - **Git 絕對乾淨**：設定檔位於本地未追蹤目錄，`git status` 永遠保持乾淨。
- **替代方案評估**：
  - *全域 OS 環境變數*：全電腦共用，易造成不同專案或不同服務同名變數互相干擾。
  - *未追蹤 YAML (`application-local.yml`)*：每次啟動需額外指定多個 active profiles，手續相對繁瑣。
  - *PowerShell 啟動腳本*：僅利於終端機執行，對 IDE 斷點偵錯整合度較低。

### 決策 2：版本控制防護與範本追蹤機制 (`.gitignore`)
- **機制設計**：
  - 在 `.gitignore` 中維持 `.vscode/` 排除，但新增白名單例外規則：
    ```gitignore
    .vscode/
    !.vscode/*.example
    ```
  - 將標準範本命名為 `.vscode/launch.json.example` 納入 Git 追蹤。
  - 開發者於本機將其複製為 `.vscode/launch.json` 並填入個人密碼，此檔案將自動被 `.gitignore` 遮蔽，杜絕誤推機密風險。

### 決策 3：雙軌模式組態設計
- 範本中提供兩組標準組態：
  1. `Spring Boot: 本機 MSSQL (含自訂密碼)`：
     - `args`: `--spring.profiles.active=mssql`
     - `env`: 注入 `DB_PASSWORD` 與 `DB_USERNAME`
  2. `Spring Boot: H2 記憶體快速測試模式 (預設)`：
     - 無需額外 profile 與密碼，直接啟動內嵌記憶體資料庫。

## Risks / Trade-offs

- **[風險 1] 開發者誤將個人密碼寫入範本檔 `.vscode/launch.json.example`**  
  → *緩解措施*：在範本檔中保留明確註解與佔位符（如 `"請替換為你的本機MSSQL密碼"`），並在開發指引中明定「僅限修改複製後的 `launch.json`」。
- **[風險 2] 新進成員未設定 `launch.json` 導致啟動疑惑**  
  → *緩解措施*：`application-mssql.yml` 保留備用預設值 `:1111`，若本機為預設配置可直接運行；若需要自訂，文件與範本提供一鍵複製與說明。
- **[風險 3] CI 流程受本機變數干擾**  
  → *緩解措施*：GitHub Actions runner 為無狀態乾淨容器，使用預設 profile 與內嵌 H2，與本機 `.vscode/` 完全隔離，天然免疫。
