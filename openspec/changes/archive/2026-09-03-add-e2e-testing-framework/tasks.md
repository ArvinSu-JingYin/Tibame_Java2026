## 1. 構建配置與測試分流設定 (Build & Test Splitting)

- [x] 1.1 在 `pom.xml` 中引入 `com.microsoft.playwright:playwright` 依賴與 `maven-failsafe-plugin` 插件配置，透過 `mvnw test-compile` 驗證依賴下載與編譯成功
- [x] 1.2 在 `src/test/resources/application-test.yml` 建立測試專屬配置檔，配置 H2 記憶體資料庫與獨立 JWT 密鑰，驗證測試 Profile 屬性正確載入

## 2. E2E 基礎架構與隔離機制 (Base Infrastructure & Isolation)

- [x] 2.1 實作 `src/test/java/com/tibame/e2e/base/TestUserFactory.java`，提供動態生成帶隨機 UUID 尾碼之測試用戶名與密碼，驗證可產生不衝突之測試用戶資料
- [x] 2.2 實作 `src/test/java/com/tibame/e2e/base/PlaywrightTestBase.java`，配置 `@SpringBootTest(webEnvironment = RANDOM_PORT)`、注入 `@LocalServerPort`，並管理 Playwright 實例與每個案例獨立之 `BrowserContext` 沙盒生命週期，驗證基底類別可正確啟動容器並初始化無痕瀏覽器沙盒

## 3. Page Object Model (POM) 封裝

- [x] 3.1 實作 `src/test/java/com/tibame/e2e/pages/BasePage.java`，封裝基礎 URL 導航、`localStorage` Token 檢查與 SweetAlert2 彈窗偵測輔助方法，驗證共用頁面動作函式
- [x] 3.2 實作 `src/test/java/com/tibame/e2e/pages/LoginPage.java`，封裝登入表單輸入、註冊分頁切換、送出動作與錯誤警示（.alert-danger）斷言，驗證登入頁面物件方法
- [x] 3.3 實作 `src/test/java/com/tibame/e2e/pages/LedgerPage.java`，封裝智慧記帳快速輸入、等待 SweetAlert2 成功彈窗、表格流水帳比對與頂部統計卡片金額提取方法，驗證工作台頁面物件方法

## 4. 中層 API 整合測試套件 (API E2E Tests)

- [x] 4.1 實作 `src/test/java/com/tibame/e2e/api/AuthApiE2ETest.java`，透過 `TestRestTemplate` 驗證用戶註冊、重複用戶名衝突 (409 Conflict)、錯誤密碼拒絕 (401) 以及成功登入取得 Bearer Token，執行測試驗證全數通過
- [x] 4.2 實作 `src/test/java/com/tibame/e2e/api/LedgerApiE2ETest.java`，驗證分類查詢、新增記帳、月度統計計算與刪除流水帳之完整業務閉環，執行測試驗證全數通過
- [x] 4.3 實作 `src/test/java/com/tibame/e2e/api/TenantIsolationSecurityE2ETest.java`，驗證用戶 B 嘗試存取或刪除用戶 A 之記帳紀錄與自訂分類時必定被攔截 (回傳 403 Forbidden 或 404 Not Found)，執行測試驗證全數通過

## 5. 頂層 UI 真機 E2E 測試套件 (Playwright UI Tests)

- [x] 5.1 實作 `src/test/java/com/tibame/e2e/ui/AuthFlowUiE2ETest.java`，透過 Playwright 啟動無頭 Chromium 測試註冊成功後登入、跳轉導航至 `/ledger`，且瀏覽器 `localStorage` 確實存放有效 Token，執行測試驗證全數通過
- [x] 5.2 實作 `src/test/java/com/tibame/e2e/ui/AccountingFlowUiE2ETest.java`，透過 Playwright 操作智慧記帳輸入（"午餐 120"）、自動等待 SweetAlert2 彈窗消失、斷言清單表格即時出現紀錄與統計卡片數字動態更新，最後點擊登出斷言清除 Token 並返回 `/login`，執行測試驗證全數通過

## 6. 品質門檻與工程標準檢驗 (Quality Gate & DoD Verification)

- [x] 6.1 執行 `./mvnw.cmd test`，確認 Maven Surefire 維持極速單元測試反饋（毫秒級），現有與新增單元測試 100% 綠燈通過
- [x] 6.2 執行 `./mvnw.cmd verify`，確認 Maven Failsafe 自動啟動 Spring Boot 隨機埠與 Playwright 瀏覽器，完整執行全套整合與 E2E 測試並 100% 綠燈通過
- [x] 6.3 執行 `openspec validate add-e2e-testing-framework --strict`，確保提案規格與工程標準完全合規
