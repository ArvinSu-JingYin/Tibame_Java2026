## 1. Specification and Governance Synchronization

- [x] 1.1 檢驗並固化 `docs/specifications/engineering_standards_and_code_cleanliness.md`，確認包含 IDE 自動化、後端代碼潔淨慣例、故障排除 SOP 與 Zero-Warning DoD 矩陣，並檢核檔案存在與語法完整性
- [x] 1.2 更新並確認 `docs/README.md` 專案文件總覽門戶，將工程標準納入主題導航矩陣，驗證相對路徑跳轉有效性
- [x] 1.3 整合 `docs/specifications/daily_ledger_system/06_quality_assurance_and_dod.md` 驗收規約，引用工程標準作為 SSOT，驗證檢核項目連結完整性

## 2. Workspace and Skill Rules Verification

- [x] 2.1 驗證 `.vscode/settings.json` 自動化配置，確認 `source.organizeImports: always` 與弱提示過濾參數有效生效
- [x] 2.2 檢查 `.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md` 與 `openspec/config.yaml`，確保代碼潔淨與零警告指引一致性

## 3. Quality Gate and Zero-Warning DoD Verification

- [x] 3.1 執行全專案 Maven 測試編譯 `mvnw clean test-compile`，確保 BUILD SUCCESS 且 0 編譯錯誤
- [x] 3.2 執行全套件單元測試 `mvnw test`，確保測試 100% 綠燈通過
- [x] 3.3 執行 OpenSpec 變更驗證 `openspec validate add-engineering-standards-and-code-cleanliness --strict`，確保規格完全合規
