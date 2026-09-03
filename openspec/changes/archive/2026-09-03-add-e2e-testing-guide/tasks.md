## 1. 測試基底與除錯架構增強

- [x] 1.1 在 `PlaywrightTestBase.java` 中實現動態有頭模式切換（讀取系統屬性 `playwright.headed` 與環境變數 `PLAYWRIGHT_HEADED`）及 400ms SlowMo 機制，並執行 `.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AccountingFlowUiE2ETest` 驗證預設無頭執行完全綠燈通過

## 2. E2E 測試操作手冊與規範撰寫

- [x] 2.1 依據探索報告成果建立正式規格文件 `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md`，完整記錄 CLI 雙軌速查清單、動態有頭除錯模式、Page Object Model 範式、10 大測試案例矩陣盤點與 FAQ 排查對策
- [x] 2.2 更新流水帳系統規格清單 `docs/specifications/daily_ledger_system/README.md` 與專案文件總門戶 `docs/README.md`，補齊第 10 號文件索引與雙向導覽連結

## 3. 全鏈路驗證與驗收

- [x] 3.1 執行全專案構建與驗收測試 `.\mvnw.cmd verify`，確保 Surefire 單元測試與 Failsafe 端到端整合測試全數通過（零回歸、零失敗）
