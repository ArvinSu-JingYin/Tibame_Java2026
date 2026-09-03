## Why

隨著專案業務邏輯與架構持續擴展，代碼庫需要具備一致的工程潔淨度慣例、IDE 工作區自動化防護機制與全套件零警告交付標準（Zero-Warning DoD）。先前已制定了權威技術文件 `docs/specifications/engineering_standards_and_code_cleanliness.md` 並更新了各模組索引與 DoD 引用，現需透過 OpenSpec 建立正式的工程標準能力規格（`engineering-standards`），將 IDE 自動化、代碼潔淨原則（如 JpaRepository 介面標註、Pattern 快取、死碼消除）與零警告檢核矩陣固化為專案的一級規格資產。

## What Changes

- **新增工程標準規格能力 (`engineering-standards`)**：建立專案全域工程標準與代碼潔淨規格，明確定義 IDE 自動化儲存配置（`source.organizeImports: always`）、弱提示過濾、後端代碼潔淨慣例（JpaRepository 無冗餘 `@Repository`、正則表達式類別常數快取、JUnit 5 測試套件可見性）以及 IDE 語言伺服器故障排除 SOP。
- **固化零警告交付檢核矩陣 (Zero-Warning DoD)**：將全套件零編譯警告、零未使用引用、零孤兒死碼、100% 單元測試綠燈納入交付規範。
- **維護文件體系與真實單一來源 (SSOT)**：確認 `docs/specifications/engineering_standards_and_code_cleanliness.md` 與專案門戶 `docs/README.md`、業務模組 `06_quality_assurance_and_dod.md` 的鏈結與約束力，確保規格與實作高度一致。

## Capabilities

### New Capabilities
- `engineering-standards`: 專案通用工程標準、代碼潔淨慣例、IDE 工作區自動化防護與 Zero-Warning 交付檢核矩陣。

### Modified Capabilities
None.

## Impact

- **受影響規格與文件**：
  - `openspec/specs/engineering-standards/spec.md` (新規格)
  - `docs/specifications/engineering_standards_and_code_cleanliness.md`
  - `docs/README.md`
  - `docs/specifications/daily_ledger_system/06_quality_assurance_and_dod.md`
- **受影響工具與配置**：
  - `.vscode/settings.json`
  - `.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md`
  - `openspec/config.yaml`
- **受影響代碼**：本變更主要為規格化與既有標準固化，前序代碼重構（移除 `@Repository`、常數快取、清理未使用引用）已完成，無重大破壞性變更（No Breaking Changes）。
