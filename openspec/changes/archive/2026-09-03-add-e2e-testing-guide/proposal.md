## Why

目前專案雖已具備完整的 E2E 測試（涵蓋 API 整合測試與 Playwright UI 真機測試），但 Playwright 測試基底預設寫死為無頭模式（Headless Mode），導致工程師在本機排查 UI 動態互動、CSS 動畫與彈窗狀態時如同「除錯黑盒」。此外，Maven 雙軌分流機制（Surefire vs Failsafe）與全鏈路 E2E 測試操作清單散落於代碼中，缺乏一份正式且結構化的操作維運手冊與案例矩陣盤點，增加團隊協作與新進工程師的上線與維運成本。

## What Changes

- **動態有頭除錯模式 (Dynamic Headed Mode)**：修改 `PlaywrightTestBase.java`，支援透過 JVM 系統屬性 `-Dplaywright.headed=true` 或環境變數 `PLAYWRIGHT_HEADED=true` 動態喚醒可見之 Chromium 視窗，並於有頭模式自動加入 400ms 微延遲（SlowMo），利於肉眼觀察與除錯。
- **E2E 測試操作與維運手冊**：在 `docs/specifications/daily_ledger_system/` 正式建立 `10_e2e_testing_guide_and_operation_manual.md`，提供 CLI 雙軌分流指令速查、IDE 單鍵除錯技巧、Page Object Model 規範、全案例矩陣盤點以及 FAQ 故障排除指南。
- **文件門戶與索引同步**：更新 `docs/specifications/daily_ledger_system/README.md` 與專案根目錄 `docs/README.md`，將 E2E 測試手冊納入日常流水帳系統正式規格文件體系中。

## Capabilities

### New Capabilities
<!-- 無新增 Capability -->

### Modified Capabilities
- `e2e-testing`: 擴充 E2E 測試規範，新增動態有頭除錯模式（支援視覺化肉眼跟隨與慢速重播）、CLI 雙軌執行分流與維運手冊規範。

## Impact

- **測試基底**：`src/test/java/com/tibame/e2e/base/PlaywrightTestBase.java` 引入動態模式切換邏輯，向後相容且不影響現有無頭 CI/CD 執行。
- **規格文件**：新增 `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md`，更新相關索引文件。
- **無生產代碼影響**：不修改任何業務邏輯與 API 介面，風險極低。
