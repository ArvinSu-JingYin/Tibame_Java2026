## Why

目前日常流水帳系統 (`daily-ledger-system`) 的技術與維運文件以 `01_...` 至 `08_...` 完整收錄在 `docs/daily_ledger_system/` 目錄中。

先前建立之單元測試操作手冊目前位於 `docs/unit_testing_guide_and_test_catalog.md`，導致 `daily_ledger_system` 缺少了單元測試與品質工程的連續性章節，無法形成完整的技術資產閉環。為了提高文件內聚性、避免檔案散落於根目錄，並讓後續開發者在查閱 `docs/daily_ledger_system/` 時能一覽從需求、架構、操作到測試的全生命週期文件，特立此提案將單元測試手冊正式編號為 `09` 納入該目錄。

## What Changes

- **搬移並重構文件**：將 `docs/unit_testing_guide_and_test_catalog.md` 遷移為 `docs/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md`。
- **對齊標題與前言規格**：
  - 更新主標題為 `# 9. 單元測試操作手冊與測試案例盤點清單 (Unit Testing Guide & Test Catalog)`。
  - 補齊對齊 `01~08` 風格之專案代號 (`daily-ledger-system`)、所屬模組與狀態 Metadata。
- **更新系統總目錄索引**：在 `docs/daily_ledger_system/README.md` 中將項目更新為同目錄內的 `09_unit_testing_guide_and_test_catalog.md`。
- **清理舊路徑檔案**：移除原根目錄之 `docs/unit_testing_guide_and_test_catalog.md`，杜絕版本分岔與雙份維護問題。

## Capabilities

### New Capabilities
*(無，本變更為純文件組織重構，已設定 `skip_specs: true`)*

### Modified Capabilities
*(無，不更動底層 API 或系統行為規格契約)*

## Impact

- **受影響檔案**：
  - `docs/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md` (新增/搬移)
  - `docs/daily_ledger_system/README.md` (更新導覽索引)
  - `docs/unit_testing_guide_and_test_catalog.md` (刪除舊路徑)
- **API 與系統影響**：無任何生產代碼或依賴異動，全套件 54 個單元測試維持全數綠燈。
