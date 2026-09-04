## Why

為了解決團隊成員本機微軟 SQL Server（MS SQL）密碼各異時，若直接修改受 Git 追蹤的 `application-mssql.yml` 會引發機密外洩風險、Git 工作區污染（Dirty Tree）、Git 提交歷史不可逆洩露以及協作合併衝突等痛點。透過標準化 IDE 行程環境變數注入（IDE Environment Injection）與 Git 隔離機制，達成「原始碼零改動、工作區零污染、開箱即用平滑切換、CI/CD 絕對隔離」的開發治理標準。

## What Changes

- **IDE 啟動組態範本**：新增 `.vscode/launch.json.example` 範本，提供「本機 MSSQL（含自訂密碼注入）」與「H2 記憶體快速測試模式」兩組預設組態，方便工程師複製使用。
- **Git 忽略與版本控制防護**：驗證並維護 `.gitignore` 規則，確保 `.vscode/` 與包含個人機密的 `launch.json` 受到絕對阻斷，避免意外提交，同時允許範本檔案受版本控管。
- **本機機密注入工程規範收錄**：在工程規範（Engineering Standards）中正式新增本機私密憑證管理、外部化屬性覆蓋順序與 IDE 啟動隔離的標準要求。
- **開發者操作指引與 CI 隔離文檔化**：在專案技術規格庫中落實本機開發資安最佳實踐與 CI 獨立性驗證報告。

## Capabilities

### New Capabilities
<!-- 無新增全新領域能力，本次為強化專案工程標準規範 -->

### Modified Capabilities
- `engineering-standards`: 新增「本機開發憑證隔離與 IDE 啟動組態規範（Local Development Credential Isolation and IDE Launch Configurations）」，要求本機資料庫密碼一律透過 IDE 行程環境變數或命令列參數注入，禁止在 Git 追蹤之 YAML 檔修改，並確保本機配置與遠端 CI/CD 完全解耦。

## Impact

- **設定檔與工具鏈**：新增 `.vscode/launch.json.example`；檢核 `.gitignore`。
- **規格與文件**：更新 `openspec/specs/engineering-standards/spec.md`（透過 delta spec）；補充相關開發手冊。
- **業務代碼與測試**：Spring Boot 原始碼零改動（Zero Code Change），現有單元測試、整合測試與 GitHub Actions CI 建置 100% 綠燈相容。
