## Why

目前專案已完整實作包含業務服務層 (`CategoryServiceTest`, `LedgerServiceTest`, `AuthServiceTest`) 與密碼學/資安模組 (`CryptoServiceTest`, `PasswordServiceTest`, `PasswordPolicyValidatorTest`, `TokenServiceTest`, `SmartParserServiceTest`) 共 54 個單元測試案例。

然而專案缺乏一份統一的「單元測試操作手冊與測試案例盤點清單」供開發團隊查閱 CLI 測試指令、AAA 撰寫範式、安全防禦矩陣與新功能開發檢核清單。為了使後續開發者能遵循標準化純單元測試原則、快速運行除錯並維護現有測試，特立此提案正式將單元測試指南與測試目錄納入專案文件標準。

## What Changes

- **新增單元測試操作手冊與盤點清單**：於 `docs/unit_testing_guide_and_test_catalog.md` 建立完整且結構化之技術指引，涵蓋：
  1. 執行摘要與純單元測試隔離哲學（零外部 DB 依賴、不啟動 Spring 容器、毫秒級即時回饋）。
  2. CLI 與 IDE 測試操作指南（Maven Wrapper 速查指令、單一類別/方法篩選、套件層級測試、IDE 單鍵執行與中斷點除錯）。
  3. AAA (Arrange-Act-Assert) 模式撰寫規範與標準代碼樣板。
  4. 全專案 54 個單元測試完整盤點目錄（包含 CategoryService 16 個、LedgerService 18 個、AuthService 3 個、Crypto 13 個、Token/Parser 4 個測試之方法名稱、測試情境與斷言檢驗目標）。
  5. 核心業務安全防禦矩陣（多租戶水平越權 IDOR 防護、系統內建分類唯讀保護、收支型別一致性防呆、孤兒記錄與 Null 安全處理）。
  6. 後續新功能開發測試檢核清單（Developer Checklist 8 項標準）。
- **更新系統文件導覽目錄**：於 `docs/daily_ledger_system/README.md` 中將 `unit_testing_guide_and_test_catalog.md` 納入文件清單索引。

## Capabilities

### New Capabilities
*(無，本變更為純文件與測試規格手冊變更，已設定 `skip_specs: true`)*

### Modified Capabilities
*(無，不變更系統行為與 API 規格契約)*

## Impact

- **受影響檔案**：
  - `docs/unit_testing_guide_and_test_catalog.md` (新增)
  - `docs/daily_ledger_system/README.md` (更新索引)
- **API 與系統影響**：無執行時程式碼或依賴異動，全套件 54 個單元測試維持 100% 綠燈通過。
