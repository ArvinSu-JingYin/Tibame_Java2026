## Why

現有日常流水帳系統（Daily Ledger System）已具備以 JUnit 5 與 Mockito 為主的單元測試，但無法覆蓋真實 HTTP/Filter 安全鏈路（Spring Security JWT 授權與過期）、多租戶資料穿透防護（跨用戶橫向越權攔截），以及前端響應式與非同步連動（Vue 3 / Axios 非同步拉取、SweetAlert2 彈窗、儀表板即時餘額刷新）。
為兼顧執行效率與真實度保證，本變更導入端到端（E2E）自動化測試體系，採用純 Java 技術棧的 Playwright Java 與 Spring Boot 隨機埠整合測試，確保完全免除 Node.js 依賴，並藉由分層測試與資料隔離策略保障高品質交付。

## What Changes

- **導入 Playwright Java 核心依賴**：在 `pom.xml` 中引入 `com.microsoft.playwright:playwright`，透過 Java 核心進程管理 Chromium 無頭瀏覽器，維持零 Node.js / npm 工具鏈純潔度。
- **配置 Maven 測試分流機制**：引入 `maven-failsafe-plugin` 綁定 `integration-test` 與 `verify` 階段，使 `*Test.java` 維持毫秒級極速反饋，`*IT.java` / `*E2ETest.java` 於驗收時執行完整驗證。
- **建立測試專用配置與獨立資料庫**：提供 `application-test.yml`，測試時於隨機埠 (`webEnvironment = RANDOM_PORT`) 啟動 Spring Boot 與獨立 H2 In-Memory DB，隔離正式與本機資料庫。
- **建立 E2E 基礎設施與隔離機制**：
  - `PlaywrightTestBase`：管理隨機 Port 注入、Playwright 實例與 `BrowserContext` 生命週期。
  - `TestUserFactory`：動態生成帶有隨機 UUID 尾碼的測試用戶，徹底消除帳號衝突。
  - `BrowserContext` 沙盒隔離：在每個案例中採用獨立無痕沙盒，隔離 Cookie 與 `localStorage`。
- **實作 Page Object Model (POM)**：封裝 `BasePage`、`LoginPage`、`LedgerPage`，封裝 DOM 選取器與 SweetAlert2 非同步等待，避免前端 UI 微調導致測試脆弱易碎。
- **實作中層 API 整合測試套件**：驗證註冊登入 Token 生成、記帳 CRUD 狀態機，以及跨租戶越權攻擊攔截（403/404）。
- **實作頂層 UI 真機 E2E 測試套件**：驗證登入跳轉至 `/ledger`、智慧快速記帳輸入、SweetAlert2 反饋、記帳列表與統計卡片即時刷新、登出清空 Token 等核心黃金路徑。
- **更新品質門檻（DoD）**：在工程規範中納入 `mvn verify` 整合驗證標準。

## Capabilities

### New Capabilities
- `e2e-testing`: 定義 API 全鏈路整合測試與 Playwright Java 瀏覽器真機 E2E 測試之規範，涵蓋金字塔分層職責、測試環境資料庫隔離、Page Object Model 封裝與核心黃金路徑驗證。

### Modified Capabilities
- `engineering-standards`: 更新 Definition of Done (DoD) 品質門檻與 Maven 測試分流標準，將 `mvn test`（單元測試極速反饋）與 `mvn verify`（整合與真機 E2E 驗證）納入正式交付規範。

## Impact

- **構建系統**：`pom.xml` 新增 `playwright` 依賴與 `maven-failsafe-plugin` 插件配置。
- **測試資源**：新增 `src/test/resources/application-test.yml`。
- **測試源碼**：於 `src/test/java/com/tibame/e2e` 下新增測試基礎架構、Page Object 類別、API E2E 與 UI E2E 測試類別。
- **相依環境**：測試執行時由 Playwright Java 自動下載與管理微軟 Chromium 瀏覽器驅動快取，無需本機安裝 Node.js。
- **現有代碼**：現有業務代碼與既有單元測試（`*Test.java`）完全不受影響，無破壞性變更（Non-breaking）。
