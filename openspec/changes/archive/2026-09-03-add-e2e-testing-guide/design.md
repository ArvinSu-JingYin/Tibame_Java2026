## Context

參見 `proposal.md`。專案已擁有完整的單元測試與端到端（E2E）測試金字塔架構，並已在 `pom.xml` 中妥善設定 Maven Surefire 與 Failsafe 雙軌隔離。然而，目前 `PlaywrightTestBase.java` 寫死無頭模式 (`setHeadless(true)`)，且缺乏正式納管的 E2E 測試維運操作指引與案例矩陣盤點。

## Goals / Non-Goals

**Goals:**
- **動態參數化有頭除錯**：在 `PlaywrightTestBase.java` 中實現免重編譯的動態有頭模式偵測，並於有頭模式注入 400ms SlowMo 動畫放慢延遲。
- **維運手冊規範化**：以正式規格手冊形式建立 `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md`，完整記錄雙軌 CLI 指令、POM 開發範式、全鏈路案例矩陣盤點及 FAQ 排錯對策。
- **文件門戶與索引同步**：更新規格文件清單 `docs/specifications/daily_ledger_system/README.md` 與專案總門戶 `docs/README.md`。

**Non-Goals:**
- 不更動任何業務功能代碼或 REST API 規格。
- 不更動 `pom.xml` 現有的 Maven Surefire 與 Failsafe 配置（已確認配置完善）。
- 不重寫現存的 5 個 E2E 測試類別邏輯。

## Decisions

### 決策 1：雙軌動態偵測機制（JVM 屬性與環境變數）
- **選擇**：同時支援 `System.getProperty("playwright.headed", "false")` 與 `System.getenv("PLAYWRIGHT_HEADED")`。
- **理由**：
  - 在 CLI 執行時，開發者習慣傳入 `-Dplaywright.headed=true`。
  - 在特定 IDE 或終端環境中，開發者亦可透過全域環境變數開關控制。
- **替代方案評估**：僅支援 `application-test.yml` 配置檔。缺點是每次想看畫面都必須修改 YAML 檔案並重新存檔，容易誤將除錯配置簽入版本庫。

### 決策 2：有頭模式自動綁定 400ms SlowMo
- **選擇**：當 `isHeaded == true` 時，將 `setSlowMo(400)`；否則為 `0`。
- **理由**：Playwright 執行速度極快，若無延遲，即使跳出瀏覽器視窗，按鈕點擊與頁面跳轉亦在數十毫秒內閃退，肉眼難以追蹤。加入 400ms 能在人體視覺舒適感與執行效率間取得最佳平衡。
- **替代方案評估**：由開發者手動在程式碼中寫入延遲。缺點是侵入性高且極易遺留脆弱的測試延遲。

### 決策 3：維運手冊編號定位為第 10 號規格
- **選擇**：命名為 `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md`。
- **理由**：既有流水帳規格目錄已有 01 至 09 號文件（08 為使用者手冊、09 為單元測試手冊），10 號自然銜接作為端到端與整合測試之正式指引，並具備完整導覽連結。

## Risks / Trade-offs

- **[Risk] CI/CD 無 GUI 環境誤觸發有頭模式**  
  → **Mitigation**：預設值嚴格為 `false`，僅在明確指定字串為 `"true"` 時才喚醒有頭模式，杜絕 headless agent 環境崩潰風險。
- **[Risk] SlowMo 導致特定長時間等待測試觸發 Timeout**  
  → **Mitigation**：400ms 為單步操作延遲，專案目前 UI E2E 測試步驟精簡，整體執行時間僅約數秒，且 Playwright 預設 step timeout 為 30 秒，完全在安全餘裕範圍內。
