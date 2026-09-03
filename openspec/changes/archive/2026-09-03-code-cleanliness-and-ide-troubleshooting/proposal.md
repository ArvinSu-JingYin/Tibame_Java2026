## Why

在專案持續演進與測試重構過程中，IDE（VS Code / Antigravity IDE）的 Problems（問題）面板累積了未使用引用（Unused Imports）、方法內重複編譯正規表達式導致的效能開銷與死碼欄位（Unused Field）、Spring Data JPA 介面上多餘的 `@Repository` 標註，以及 Spring Boot Tools 擴充套件產生的版本生命週期警告（BOOT_VERSION_VALIDATION_CODE）。

這些警告雖然不會中斷 Maven 底層建置與測試，但會造成認知負載、潛在運行期 GC 壓力，以及專案慣例不一致。因此需要透過本次變更落實代碼潔淨化、配置 IDE 自動化儲存防護、固化技能庫規範與交付檢核標準（DoD）。

## What Changes

- **移除多餘標註**：自 `UserRepository`、`CategoryRepository` 與 `AccountRecordRepository` 介面移除多餘的 `@Repository` 註解及其引用。
- **優化正則表達式編譯與清理死碼**：重構 `RegexSmartParserServiceImpl` 中的金額正則表達式，確保編譯快取於靜態常數 `AMOUNT_PATTERN`，移除方法內部重複 `Pattern.compile`，消除死碼欄位警告並提升高併發解析效能。
- **清理測試類別未使用的引用**：清理 `AuthServiceTest`、`LedgerServiceTest` 與 `TokenServiceTest` 中多餘或未使用的 `import` 陳述式。
- **配置 IDE 存檔自動組織引用**：於 `.vscode/settings.json` 配置 `editor.codeActionsOnSave` 支援 `source.organizeImports: always`，實現每次存檔自動移除無用引用。
- **消除 Spring Tools 版本生命週期雜訊**：於 `.vscode/settings.json` 配置 `"boot-java.validation.java.version-validation": "OFF"`，並確認 `pom.xml` 中 Spring Boot Starter Parent 升級至穩定的 `3.3.13` 修補版本。
- **固化技能庫規範與 Definition of Done (DoD)**：更新 `.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md` 與 `openspec/config.yaml`，增補 Repository 介面原則、正則快取原則、單元測試修飾詞規範與「零警告檢核」驗收清單。

## Capabilities

### New Capabilities
None.（本變更屬於代碼潔淨度重構、IDE 自動化工具配置與規範固化，不新增使用者功能規格）

### Modified Capabilities
None.（本變更不變更現有業務系統之功能規格與外部 API 行為，已於 `.openspec.yaml` 宣告 `skip_specs: true`）

## Impact

- **受影響代碼**：
  - `com.tibame.repository.UserRepository`
  - `com.tibame.repository.CategoryRepository`
  - `com.tibame.repository.AccountRecordRepository`
  - `com.tibame.service.impl.RegexSmartParserServiceImpl`
  - `com.tibame.service.AuthServiceTest`
  - `com.tibame.service.LedgerServiceTest`
  - `com.tibame.common.crypto.token.TokenServiceTest`
- **受影響配置與工具**：
  - `.vscode/settings.json`
  - `pom.xml`
  - `.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md`
  - `openspec/config.yaml`
- **相容性與重大變更**：無 BREAKING CHANGES，既有業務邏輯、API 行為與資料結構完全相容，單元測試維持 100% 通過。
