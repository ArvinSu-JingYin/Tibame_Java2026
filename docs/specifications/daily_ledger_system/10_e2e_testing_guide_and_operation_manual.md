# 10. 端到端 (E2E) 測試操作手冊與維運指南 (E2E Testing Guide & Operation Manual)

> **專案代號**：`daily-ledger-system`  
> **所屬模組**：端到端整合測試、Playwright 真機 UI 驗收、動態有頭除錯模式、Page Object Model 範式與案例盤點  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / Playwright Java 1.46+ / Maven Failsafe / Chromium / H2 In-Memory  
> **測試統計**：5 個 E2E 測試類別，共 10 大核心端到端情境（涵蓋 3 個 API 整合測試類別 + 2 個 UI 真機測試類別）  
> **文件狀態**：正式發布 (`Active`)  
> **目標讀者**：全端開發者、測試架構師、QA 自動化工程師與維運人員  
> **導覽指引**：[← 返回流水帳規格目錄 (README.md)](README.md) ｜ [← 上一篇：09 單元測試手冊](09_unit_testing_guide_and_test_catalog.md) ｜ [← 返回專案總門戶 (docs/README.md)](../../README.md)  

---

## 1. 執行摘要與設計哲學 (Executive Summary & Philosophy)

本專案「日常流水帳系統（Daily Ledger System）」構建了兼顧「極速反饋」與「真實鏈路驗證」的現代化測試金字塔體系。

在金字塔頂層，我們引入微軟開源之 **Playwright Java** 驅動真實 Chromium 瀏覽器，結合標準 **Page Object Model (POM)**，提供跨瀏覽器、端到端的真機驗收能力；在中層則以 Spring Boot `RANDOM_PORT` 啟動真實嵌入式 Tomcat 容器，檢驗完整的安全攔截、JWT 簽驗、業務交易與多租戶資料隔離防線。

```
+---------------------------------------------------------------------------------------------------+
|                                專案測試金字塔與沙盒隔離執行架構                                     |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   [頂層 UI 真機 E2E]       Playwright Chromium + POM 封裝                                         |
|   (2 類別 / 真機端到端)    - AuthFlowUiE2ETest (註冊/登入/LocalStorage Token 檢驗/登出)            |
|                            - AccountingFlowUiE2ETest (自然語言快速記帳/Swal2/統計卡片刷新)        |
|                                     ^                                                             |
|   [中層 API 全鏈路 E2E]    Spring Boot (RANDOM_PORT) + TestRestTemplate                           |
|   (3 類別 / 整合安全鏈路)  - TenantIsolationSecurityE2ETest (跨租戶越權攔截 403/404)              |
|                            - LedgerApiE2ETest (記帳 CRUD 與統計聚合)                              |
|                            - AuthApiE2ETest (註冊/登入狀態機與 Token 簽發)                        |
|                                     ^                                                             |
|   [底層純單元測試]         JUnit 5 + Mockito (54 個案例，由 surefire 毫秒級極速執行)              |
|                                                                                                   |
|   [沙盒隔離防護機制]                                                                               |
|   - 埠號隔離：@SpringBootTest(webEnvironment = RANDOM_PORT)，徹底消除 8080 連接埠碰撞              |
|   - 資料庫隔離：H2 In-Memory DB (application-test.yml)，測試結束隨 Context 自動銷毀               |
|   - 使用者隔離：TestUserFactory 隨機動態後綴 (UUID)，杜絕 409 Conflict 帳號衝突                   |
|   - 瀏覽器隔離：BrowserContext 無痕獨立實例，每個測試案例全新隔離 LocalStorage 與 Cookie           |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

### 核心設計哲學
1. **雙軌分流 (Dual-Track Separation)**：日常開發執行單元測試維持 2~5 秒極速反饋；驗收與交付時啟動全套 E2E 驗證，兩者涇渭分明。
2. **沙盒零污染 (Hermetic Sandbox)**：連接埠隨機指派、資料庫記憶體化、測試帳號隨機生成、無痕沙盒上下文，確保單一案例失敗絕不引發骨牌效應。
3. **無黑盒除錯 (Zero Black-box Debugging)**：支援動態喚醒有頭瀏覽器（Headed Mode）與 SlowMo 降速播放，徹底告別傳統 UI 測試排錯全憑日誌猜測之困境。

---

## 2. Maven 雙軌分流機制與 CLI 操作速查 (Maven Surefire vs Failsafe)

專案在 `pom.xml` 中採用 Maven 標準的雙軌分流插件架構：

```
+-----------------------------------------------------------------------------------+
|                           Maven 雙軌測試分流生命週期                               |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   日常存檔 / 極速單元反饋                                                          |
|          |                                                                        |
|          v                                                                        |
|   [ .\mvnw.cmd test ]                                                             |
|          |                                                                        |
|          +--> maven-surefire-plugin (排除 **/*IT.java, **/*E2ETest.java)          |
|               - 僅執行 54 個純單元測試                                            |
|               - 不啟動 Spring 容器、不連 DB、不開瀏覽器 (2~5 秒完成)               |
|                                                                                   |
|   驗收階段 / 全鏈路整合交付                                                        |
|          |                                                                        |
|          v                                                                        |
|   [ .\mvnw.cmd verify ]                                                           |
|          |                                                                        |
|          +--> phase: test (先由 Surefire 驗證單元測試全部綠燈)                    |
|          |                                                                        |
|          +--> phase: integration-test (由 Failsafe 啟動 E2E 整合測試)             |
|          |    - 啟動 Spring Boot RANDOM_PORT 容器與 H2 記憶體資料庫                |
|          |    - 喚醒 Playwright Chromium 瀏覽器進程 (免額外 Node.js 環境)          |
|          |    - 依序執行 *IT.java 與 *E2ETest.java                                |
|          |                                                                        |
|          +--> phase: verify (由 Failsafe 彙整整合測試驗收結果)                     |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### CLI 命令列速查清單 (PowerShell / Bash)

> [!NOTE]
> 在 Windows PowerShell 環境下，傳入包含句點之 JVM 參數（如 `-Dit.test=...`）時，建議將參數整體加上雙引號（例如 `"-Dit.test=AccountingFlowUiE2ETest"`），以避免 PowerShell 誤解析語法。

```powershell
# 1. 執行全專案全套驗證 (包含 54 個單元測試 + 3 個 API 整合測試 + 2 個 UI 真機測試)
.\mvnw.cmd verify

# 2. 僅編譯並執行整合與 E2E 測試 (跳過單元測試重複執行，大幅縮減除錯等待)
.\mvnw.cmd test-compile failsafe:integration-test

# 3. 指定單一 E2E 測試類別執行 (使用 it.test 參數)
# (1) 記帳核心業務 UI 真機流程驗收
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest"

# (2) 註冊登入與 Token 持久化 UI 真機驗收
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AuthFlowUiE2ETest"

# (3) 跨租戶橫向越權 (IDOR) 防護 API 整合驗收
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=TenantIsolationSecurityE2ETest"

# (4) 記帳與統計 API 整合測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=LedgerApiE2ETest"

# (5) 認證狀態機與 Token 簽發 API 整合測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AuthApiE2ETest"

# 4. 指定特定測試方法執行 (語法：類別名#方法名)
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest#testAccountingFlow"

# 5. 使用萬用字元批次執行測試分組
# (1) 執行所有 UI 真機測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=*UiE2ETest"

# (2) 執行所有 API 整合測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=*ApiE2ETest"

# 6. 開啟詳細除錯日誌 (Debug Trace)
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest" -X
```

---

## 3. 動態有頭模式 (Dynamic Headed Mode) 與視覺化除錯指引

### 3.1 核心痛點與切換架構
預設無頭模式 (`headless: true`) 極其適合 CI/CD 與自動化構建，但在開發者本機排查按鈕點擊無效、SweetAlert2 彈窗遮蔽或 Vue 狀態渲染不同步時，缺乏即時畫面會大幅增加排錯成本。

專案於 `PlaywrightTestBase.java` 中實現雙軌動態偵測機制：
- 支援 JVM 系統屬性 `playwright.headed`（CLI 推薦）
- 支援作業系統環境變數 `PLAYWRIGHT_HEADED`（終端或 IDE 推薦）
- 當偵測到開啟有頭模式時，自動注入 **400ms SlowMo (慢速重播延遲)**，使每個瀏覽器操作動作平緩呈現，利於肉眼觀察追蹤。

```java
// PlaywrightTestBase.java 核心動態切換邏輯
boolean isHeaded = Boolean.parseBoolean(System.getProperty("playwright.headed", "false"))
        || Boolean.parseBoolean(System.getenv("PLAYWRIGHT_HEADED"));

BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
        .setHeadless(!isHeaded);

if (isHeaded) {
    launchOptions.setSlowMo(400); // 喚醒有頭模式時自動放慢 400 毫秒，便於視覺追蹤
}

browser = playwright.chromium().launch(launchOptions);
```

### 3.2 喚醒有頭除錯指令
除錯時只需在指令後附加 `"-Dplaywright.headed=true"`：

```powershell
# 以可見之 Chromium 視窗執行記帳 UI 流程，並放慢動作便於觀察 DOM 變化
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest" "-Dplaywright.headed=true"
```

### 3.3 IDE 單鍵除錯技巧 (IntelliJ IDEA / VS Code / Antigravity IDE)
1. **直接執行**：於 IDE 開啟 `AccountingFlowUiE2ETest.java` 或 `AuthFlowUiE2ETest.java`，點擊測試方法左側的執行箭頭即可直接以預設無頭模式運行。
2. **啟用有頭畫面**：在 IDE 的 **Run/Debug Configurations** 中的 **VM Options** 欄位填入：
   ```text
   -Dplaywright.headed=true
   ```
3. **中斷點凍結畫面**：在 Controller、Service 或 Page Object 內部設置中斷點以 Debug 模式啟動。當斷點命中時，Playwright 會暫停腳本推進，此時瀏覽器視窗會**完全凍結在當下互動狀態**，開發者可直接利用瀏覽器開發者工具 (F12) 進行 DOM 檢視與 CSS 調試。

---

## 4. Page Object Model (POM) 架構規範與非同步處理

專案嚴格貫徹 **Page Object Model (POM)** 設計模式，杜絕測試程式碼中散落脆弱的 CSS 選擇器，將 UI 互動與業務斷言分層解耦：

```
src/test/java/com/tibame/e2e/pages/
├── BasePage.java       # 頂層抽象：封裝導航、LocalStorage 憑據存取、SweetAlert2 動畫等待
├── LoginPage.java      # 認證頁面：封裝登入/註冊表單輸入、Tab 切換、提交與錯誤提示檢驗
└── LedgerPage.java     # 記帳頁面：封裝智慧輸入框快速記帳、表格即時查詢、卡片數字提取、登出
```

### 4.1 核心設計原則
1. **Playwright 原生 Auto-waiting（嚴格禁止 `Thread.sleep`）**：
   - 傳統測試腳本常因等待動畫而加入 `Thread.sleep(1000)`，造成測試不穩定且大幅拖慢執行效率。
   - Playwright 原生具備智慧自動等待機制（在點擊前自動確認可見性、啟用狀態、未被覆蓋與穩定無動畫位移）。
2. **非同步彈窗等待封裝 (`BasePage`)**：
   - SweetAlert2 彈窗具備淡入淡出過渡效果，`BasePage` 專屬封裝安全等待方法：
     ```java
     public void waitForSweetAlert(double timeoutMs) {
         page.waitForSelector(".swal2-container, .swal2-popup, .swal2-toast", 
                 new Page.WaitForSelectorOptions()
                     .setState(WaitForSelectorState.VISIBLE)
                     .setTimeout(timeoutMs));
     }
     ```
3. **前端持久化狀態機檢驗**：
   - 透過 `page.evaluate("() => localStorage.getItem('jwt_token')")` 直接檢驗瀏覽器本機儲存空間，確保認證成功時 Token 確實被寫入，且在登出時確實被完全清除。

---

## 5. 全鏈路 10 大 E2E 測試案例矩陣盤點 (E2E Test Catalog Matrix)

專案目前具備 5 個核心 E2E 測試類別，涵蓋後端全鏈路與前端真機驗證：

| # | 測試類別名稱 | 測試層級 | 測試方法與情境說明 | 核心檢驗斷言目標 |
| :--- | :--- | :--- | :--- | :--- |
| **1** | `AuthApiE2ETest` | API 整合 | `testRegisterAndLoginSuccess()`<br>測試使用者註冊與登入成功全流程 | 註冊回傳 HTTP 201，登入回傳 HTTP 200 並成功簽發有效 JWT 格式 |
| **2** | `AuthApiE2ETest` | API 整合 | `testLoginFailureWrongPassword()`<br>測試錯誤密碼登入拒絕情境 | 密碼不正確時回傳 HTTP 401 Unauthorized，驗簽攔截生效 |
| **3** | `AuthApiE2ETest` | API 整合 | `testRegisterDuplicateUsername()`<br>測試帳號重複註冊防護 | 重複 username 嘗試註冊時回傳 HTTP 409 Conflict，資料庫唯一鍵防護生效 |
| **4** | `AuthApiE2ETest` | API 整合 | `testAccessProtectedEndpointWithoutToken()`<br>測試未授權存取保護端點 | 未攜帶 Bearer Token 存取 `/api/v1/records` 時回傳 HTTP 401 或 403 拒絕 |
| **5** | `LedgerApiE2ETest` | API 整合 | `testRecordCrudFlow()`<br>測試流水帳記錄完整 CRUD 生命週期 | 驗證新增記帳、讀取單筆、更新記錄內容、物理刪除後查詢回傳 404 |
| **6** | `LedgerApiE2ETest` | API 整合 | `testLedgerSummaryCalculation()`<br>測試月度收支聚合統計運算 | 驗證新增多筆收入與支出後，Summary API 準確聚合計算總收入、總支出與淨結餘 |
| **7** | `TenantIsolationSecurityE2ETest` | API 安全 | `testCrossTenantRecordAccessBlocked()`<br>測試跨租戶流水帳水平越權讀取 | 使用者 A 嘗試以 IDOR 查詢使用者 B 之記帳記錄時，必須回傳 HTTP 404 或 403 嚴格攔截 |
| **8** | `TenantIsolationSecurityE2ETest` | API 安全 | `testCrossTenantCategoryModificationBlocked()`<br>測試跨租戶自訂分類竄改與刪除 | 使用者 A 嘗試修改或刪除使用者 B 的自訂分類時，必須回傳 HTTP 403 Forbidden 堅決拒絕 |
| **9** | `AuthFlowUiE2ETest` | UI 真機 | `testAuthFlow()`<br>驗證註冊、自動填寫、登入跳轉與 LocalStorage | 註冊成功後自動切換登入頁、填入密碼、點擊登入、驗證自動跳轉 `/ledger` 且 LocalStorage 取得 Token，點擊登出後 Token 被清除 |
| **10** | `AccountingFlowUiE2ETest` | UI 真機 | `testAccountingFlow()`<br>驗證智慧自然語言記帳、表格即時渲染與卡片連動 | 在焦點列輸入自然語言快捷記帳、偵測 SweetAlert2 提示、檢查清單即時出現新資料行、收支儀表板卡片金額即時累加刷新 |

---

## 6. 實務常見問題排查指南 (FAQ & Troubleshooting)

### Q1: 首次執行時下載 Chromium 失敗或逾時？
- **原因**：Playwright Java 首次啟動時需自微軟官方伺服器下載 Chromium 二進位檔（約 150MB）。若在受限制的企業內網或離線環境，可能觸發網路逾時中斷。
- **排查對策**：
  1. 驅動預設存放於本機快取目錄：
     - Windows: `%LOCALAPPDATA%\ms-playwright`
     - Linux / macOS: `~/.cache/ms-playwright`
  2. 若環境具備外部代理，可於終端配置環境變數 `set HTTPS_PROXY=http://proxy-server:port` 後重試。
  3. 驅動下載成功一次後將永久快取，後續所有測試皆在毫秒級極速喚醒。

### Q2: 為什麼打 `.\mvnw.cmd test` 沒有看到任何 E2E 測試被執行？
- **原因**：這是架構設計的刻意分流！Surefire 插件排除了 `**/*IT.java` 與 `**/*E2ETest.java`，確保日常存檔編譯時單元測試維持 2~5 秒毫秒級反饋。
- **排查對策**：若要執行 E2E 測試，請使用 `.\mvnw.cmd verify`（全套驗證）或 `.\mvnw.cmd test-compile failsafe:integration-test`（快速整合測試）。

### Q3: 測試執行中出現 409 Conflict 使用者名稱重複錯誤？
- **原因**：多個測試案例使用硬編碼之固定帳號（如 `testuser`）向資料庫註冊，造成連續執行時衝突。
- **排查對策**：所有測試一律透過 `TestUserFactory.createRandomUser()` 動態生成帶有隨機 UUID 尾碼的帳號，確保測試案例 100% 具備冪等性與獨立性。

### Q4: 隨機埠是否可能發生碰撞？
- **對策**：測試統一標註 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`，由作業系統核心動態指派未被佔用的 Port，並透過 `@LocalServerPort` 注入，完全杜絕 8080 碰撞。

---

## 7. 團隊 E2E 測試開發與維運檢核清單 (Developer Checklist)

未來擴充新的 API 整合測試或 Playwright UI 真機驗收案例時，請務必遵循以下交付標準：

```
+-----------------------------------------------------------------------------------+
|                         新增 E2E 測試案例檢核清單                                   |
+-----------------------------------------------------------------------------------+
| [ ] 1. 命名規範: 類別名稱必須以 IT 或 E2ETest 結尾，確保納入 Failsafe 生命週期      |
| [ ] 2. 隨機埠規範: 必須宣告 @SpringBootTest(webEnvironment = RANDOM_PORT)         |
| [ ] 3. 帳號隨機性: 帳號一律使用 TestUserFactory 動態建立，嚴禁硬編碼固定名稱       |
| [ ] 4. POM 封裝原則: UI 測試嚴禁直接在測試方法內操作底層選擇器，一律封裝於 Page 物件|
| [ ] 5. 等待防禦原則: 嚴禁使用 Thread.sleep()，一律使用 Playwright 原生自動等待機制  |
| [ ] 6. 動態有頭相容: 繼承 PlaywrightTestBase，確保支援 -Dplaywright.headed 視覺除錯|
| [ ] 7. 繁體中文命名: 測試方法一律使用清晰的 @DisplayName 標明業務行為與預期結果    |
| [ ] 8. 本地全量綠燈: 送出 PR 前在本機執行 .\mvnw.cmd verify，確認零回歸、零失敗   |
+-----------------------------------------------------------------------------------+
```
