## Context

現有日常流水帳系統已具備 JUnit 5 + Mockito 單元測試體系，但對於 Spring Security 授權攔截、多租戶隔離防護以及 Vue 3 / Axios / SweetAlert2 前端響應式非同步互動缺乏端到端驗證（參見 `proposal.md` 與 `docs/explorations/e2e_testing_design_and_playwright_exploration.md`）。
本設計旨在以純 Java 技術棧（Zero-Node.js）導入 Playwright Java 與 Spring Boot 隨機埠整合測試，落實三層測試金字塔體系。

## Goals / Non-Goals

**Goals:**
- **Zero-Node.js 工具鏈純潔度**：完全透過 Java Process 管理微軟官方 Playwright Driver 與 Chromium 瀏覽器，免除 Node.js、npm 或 `package.json` 的維護負擔。
- **Maven 雙軌測試分流**：確保平日存檔與編譯（`mvn test`）維持毫秒級反饋，僅在驗收與 CI 階段（`mvn verify`）執行完整的整合與真機 E2E 驗收。
- **完全環境與資料隔離**：使用 H2 In-Memory DB（`application-test.yml`）、隨機埠（`RANDOM_PORT`）、動態測試用戶（`TestUserFactory`）與無痕沙盒（`BrowserContext`），達成 100% 冪等性且無殘留。
- **高內聚低耦合 Page Object Model (POM)**：將 DOM 選取器與非同步等待（Auto-waiting、SweetAlert2 捕捉）封裝在 Page Object 中，避免前端 UI 微調導致測試大面積損壞。

**Non-Goals:**
- **跨瀏覽器矩陣全面測試**：初期專注於 Chromium Headless 瀏覽器之穩定驗收，暫不延伸至 Firefox 或 WebKit。
- **壓力與效能測試 (Load/Stress Testing)**：本框架專注於功能性與安全性端到端斷言，不包含 JMeter / Gatling 等百萬級高併發效能壓測。
- **第三方外部服務 Mock**：本系統無外部付費第三方支付或簡訊服務依賴，全鏈路可直接在本地容器內閉環完成。

## Decisions

### 1. 選型決策：Playwright Java vs Selenium / Cypress / Node.js Playwright
- **決定**：採用 `com.microsoft.playwright:playwright` (1.46+)。
- **理由**：
  - **Zero-Node.js**：自動下載驅動二進位快取，徹底免除本機 Node.js / npm 環境依賴。
  - **原生 Auto-waiting**：自動等待元素進入 Actionable 狀態（可見、可點擊、啟用），完美適應 Vue 3 + Axios 的非同步響應，消除脆落的 `Thread.sleep`。
  - **無痕 Context 隔離**：提供毫秒級建立的 `BrowserContext`，輕鬆做到案例間 LocalStorage 與 Cookie 隔離。
- **替代方案**：
  - *Selenium WebDriver*：需手動管理 ChromeDriver 與繁瑣的 WebDriverWait，容易產生 Flaky Tests。
  - *Node.js Playwright / Cypress*：需額外引入 Node.js 工具鏈與前端套件管理，增加 Java 開發者的認知負擔與 CI 配置複雜度。

### 2. 資料庫與環境隔離：Spring Boot RANDOM_PORT + H2 In-Memory DB
- **決定**：測試環境使用專屬 `application-test.yml`（Profile: `test`），搭配 `@SpringBootTest(webEnvironment = RANDOM_PORT)`。
- **理由**：
  - 避免測試與開發用 MSSQL 發生連線搶佔或殘留髒資料。
  - 隨機埠動態注入 `@LocalServerPort`，完全消除固定 Port 8080 被佔用的碰撞風險。
  - H2 於測試啟動時自動載入 `schema.sql` 與 `data.sql`（包含 10 大預設分類），測試完畢隨 Spring Context 自動銷毀。
- **替代方案**：
  - *Testcontainers (Docker MSSQL)*：需本機常駐 Docker Daemon，啟動耗時（~30-60s）且資源開銷高；H2 僅需幾百毫秒。

### 3. 測試生命週期與分流：Maven Surefire + Maven Failsafe
- **決定**：
  - `maven-surefire-plugin`（預設）：執行 `*Test.java`（純單元測試，毫秒級）。
  - `maven-failsafe-plugin`：綁定 `integration-test` 與 `verify` phase，執行 `*IT.java` 與 `*E2ETest.java`。
- **理由**：確保工程師在平日開發與存檔（`mvn test`）時不會被瀏覽器啟動延遲中斷思緒，僅在正式驗證（`mvn verify`）才拉起全套整合測試。

### 4. 測試目錄與 POM 架構規劃
- **目錄層級**：
  ```
  src/test/java/com/tibame/
    └── e2e/
         ├── base/
         │    ├── PlaywrightTestBase.java       # 注入隨機埠、管理 Playwright/BrowserContext 生命週期
         │    └── TestUserFactory.java          # 動態生成帶 UUID 的測試用戶 (避免 409 衝突)
         ├── pages/                             # Page Object 封裝
         │    ├── BasePage.java                 # 基礎導航、Token 檢驗、SweetAlert2 捕捉
         │    ├── LoginPage.java                # 登入頁、註冊切換、錯誤提示斷言
         │    └── LedgerPage.java               # 快速記帳、表格即時查詢、統計卡片數值提取
         ├── api/                               # 中層 API 整合測試 (TestRestTemplate)
         │    ├── AuthApiE2ETest.java           # 註冊、登入、Token 生成與過期
         │    ├── LedgerApiE2ETest.java         # 記帳 CRUD、月度統計聚合
         │    └── TenantIsolationSecurityE2ETest.java # 跨用戶橫向越權防護 (403/404)
         └── ui/                                # 頂層 UI 真機 E2E 測試 (Playwright)
              ├── AuthFlowUiE2ETest.java        # 註冊 -> 登入 -> 跳轉 /ledger
              └── AccountingFlowUiE2ETest.java  # 智慧快速記帳 -> Swal2 彈窗 -> 卡片即時刷新 -> 登出
  ```

## Risks / Trade-offs

- **[Risk: 首次執行下載瀏覽器耗時]**
  - *風險*：Playwright 首次執行時需從微軟官方伺服器下載 Chromium 無頭瀏覽器二進位檔（約 150MB）。
  - *緩解措施*：二進位檔下載後會永久快取在使用者目錄（例如 `C:\Users\<user>\AppData\Local\ms-playwright`），後續執行將直接重複利用，幾百毫秒即可喚醒。
- **[Risk: 非同步渲染導致測試 Flaky]**
  - *風險*：Vue 3 與 Axios 經由非同步請求更新 DOM，傳統斷言可能在 DOM 尚未渲染完成時失敗。
  - *緩解措施*：全面使用 Playwright 原生 Locator 斷言（如 `page.waitForSelector(...)`）與 POM 封裝，自動輪詢等待至元素出現或逾時，嚴格禁止 `Thread.sleep`。
- **[Risk: 資料庫多用戶並行衝突]**
  - *風險*：若多個測試案例使用固定名稱 `testuser`，將觸發唯一鍵衝突。
  - *緩解措施*：`TestUserFactory` 一律使用 `test_user_${UUID.randomUUID().toString().substring(0,8)}` 動態註冊，確保每個測試案例的數據完全獨立。
