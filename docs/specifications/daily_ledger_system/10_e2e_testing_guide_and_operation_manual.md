# 10. 端到端 (E2E) 測試操作手冊與維運指南 (E2E Testing Guide & Operation Manual)

> **專案代號**：`daily-ledger-system`  
> **文件版本**：v2.0.0  
> **所屬模組**：端到端整合測試、Playwright 真機 UI 驗收、瑞士風格四大分頁工作台 POM 適配、動態有頭除錯模式與 13 大全鏈路案例盤點  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / Playwright Java 1.46+ / Maven Failsafe / Chromium / H2 In-Memory / Vue 3 MVVM / Swiss Style  
> **測試統計**：5 個核心 E2E 測試類別，10 大現行落地案例 + 3 大擴充驗收案例（共 13 案例矩陣）  
> **文件狀態**：正式發布 (`Active`)  
> **目標讀者**：全端開發者、測試架構師、QA 自動化工程師與維運人員  
> **導覽指引**：[← 返回流水帳規格目錄 (README.md)](README.md) ｜ [← 上一篇：09 單元測試手冊](09_unit_testing_guide_and_test_catalog.md) ｜ [← 返回專案總門戶 (docs/README.md)](../../README.md)  

---

## 1. 執行摘要與架構演進 (Executive Summary & Architecture Evolution)

本專案「日常流水帳系統（Daily Ledger System）」構建了兼顧「極速反饋」與「真實鏈路驗證」的現代化測試金字塔體系。

在金字塔頂層，我們引入微軟開源之 **Playwright Java** 驅動真實 Chromium 瀏覽器，結合標準 **Page Object Model (POM)**，提供跨瀏覽器、端到端的真機驗收能力；在中層則以 Spring Boot `RANDOM_PORT` 啟動真實嵌入式 Tomcat 容器，檢驗完整的安全攔截、JWT 簽驗、業務交易與多租戶資料隔離防線。

```
+---------------------------------------------------------------------------------------------------+
|                                專案測試金字塔與沙盒隔離執行架構                                     |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   [頂層 UI 真機 E2E]       Playwright Chromium + POM 封裝                                         |
|   (真機端到端驗收)         - AuthFlowUiE2ETest (註冊/登入/LocalStorage Token 檢驗/登出)            |
|                            - AccountingFlowUiE2ETest (智慧記帳/自動流轉/明細渲染/統計更新)         |
|                            - [規劃] StructuredEntry / LedgerFilter / CategoryLifecycle 擴充案例   |
|                                     ^                                                             |
|   [中層 API 全鏈路 E2E]    Spring Boot (RANDOM_PORT) + TestRestTemplate                           |
|   (整合安全鏈路驗證)       - TenantIsolationSecurityE2ETest (跨租戶越權攔截 403/404)              |
|                            - LedgerApiE2ETest (記帳 CRUD 與統計聚合)                              |
|                            - AuthApiE2ETest (註冊/登入狀態機與 Token 簽發)                        |
|                                     ^                                                             |
|   [底層純單元測試]         JUnit 5 + Mockito (66 個測試案例，由 surefire 毫秒級極速執行)          |
|                                                                                                   |
|   [沙盒隔離防護機制]                                                                               |
|   - 埠號隔離：@SpringBootTest(webEnvironment = RANDOM_PORT)，徹底消除 8080 連接埠碰撞              |
|   - 資料庫隔離：H2 In-Memory DB (application-test.yml)，測試結束隨 Context 自動銷毀               |
|   - 使用者隔離：TestUserFactory 隨機動態後綴 (UUID)，杜絕 409 Conflict 帳號衝突                   |
|   - 瀏覽器隔離：BrowserContext 無痕獨立實例，每個測試案例全新隔離 LocalStorage 與 Cookie           |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

### 1.1 工作台演進：瑞士風格四大分頁工作台 (Swiss Tabs)
前端頁面已由早期的單頁垂直堆疊佈局全面重構升級為**瑞士風格四大分頁工作台**：

```
+---------------------------------------------------------------------------------------------------+
|                               四大分頁工作台 (Swiss Tabs) 互動流轉模型                              |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   +-------------------------------------------------------------------------------------------+   |
|   | 頂部導航列: SYS-LEDGER // USER: test_user | [分類管理按鈕] | [登出按鈕]                   |   |
|   +-------------------------------------------------------------------------------------------+   |
|   | 分頁標籤: [01 記帳錄入]  |  [02 交易明細]  |  [03 財務概覽]  |  [04 分類管理]             |   |
|   +-------------------------------------------------------------------------------------------+   |
|                                                                                                   |
|   [01 記帳錄入] (QUICK ENTRY)                                                                     |
|   - 登入後預設分頁，自動聚焦金額輸入框 (#quickAmountInput)                                        |
|   - 雙模切換：結構化錄入 vs 自然語言解析 (NLP)                                                    |
|   - 記帳成功觸發 Swiss Toast，並平滑自動流轉 (Auto Transition) 至 [02 交易明細]                  |
|          |                                                                                        |
|          +==================== 自動流轉 (activeTab = 'history') ====================>+            |
|                                                                                      |            |
|                                                                                      v            |
|   [02 交易明細] (LEDGER HISTORY) <---------------------------------------------------+            |
|   - 即時展示最新流水帳記錄與收支標籤                                                              |
|   - 提供收支類型、分類、起訖日期、關鍵字之多維度篩選器                                            |
|   - 分頁控制器、編輯 Modal 彈窗與刪除二次確認對話框                                               |
|                                                                                                   |
|   [03 財務概覽] (FINANCIAL ANALYTICS)                                                             |
|   - 年份與月份控制器 (上一月 / 下一月 / 本月重設)                                                 |
|   - 三大財務統計卡片：總收入 (.swiss-stat-income)、總支出 (.swiss-stat-expense)、淨結餘           |
|   - 【注意】處於 v-show 隱藏狀態時，元素於 DOM 中 display: none，Playwright 預設視為不可見        |
|                                                                                                   |
|   [04 分類管理] (CATEGORY MANAGEMENT)                                                             |
|   - 獨立平鋪管理視圖（取代舊版狹窄 Modal 彈窗）                                                   |
|   - 新增自訂收支分類表單與圖標選擇器                                                              |
|   - 分類表格：系統內建分類 (唯讀保護) vs 使用者自訂分類 (支援刪除操作)                           |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

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
|               - 僅執行 66 個純單元測試                                            |
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

### 2.1 【重大避坑規範】Windows PowerShell 參數雙引號規則
在 Windows PowerShell 環境下執行 Maven 命令時，若傳入包含減號或句點的系統屬性（如 `-Dit.test=...`），PowerShell 會將 `-D` 與後續參數截斷，導致 Maven 丟出以下生命週期錯誤：
```text
[ERROR] Unknown lifecycle phase ".test=AccountingFlowUiE2ETest". You must specify a valid lifecycle phase...
```
**強制規範**：所有包含 `-D` 屬性的參數必須**以雙引號整體括起**（例如 `"-Dit.test=AccountingFlowUiE2ETest"`）！

### 2.2 CLI 命令列速查清單 (PowerShell / CMD / Bash)

```powershell
# 1. 執行全專案全套驗證 (包含 66 個單元測試 + 3 個 API 整合測試 + 2 個 UI 真機測試 + IT 整合測試)
.\mvnw.cmd verify

# 2. 僅編譯並執行整合與 E2E 測試 (跳過單元測試重複執行，大幅縮減除錯等待)
.\mvnw.cmd test-compile failsafe:integration-test

# 3. 指定單一 E2E 測試類別執行 (雙引號括起 -Dit.test 參數)
# (1) 記帳核心業務 UI 真機流程驗收 (含四大分頁流轉與卡片提取)
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

# 6. 開啟動態有頭模式 (Headed Mode) 觀察畫面互動 (附加 "-Dplaywright.headed=true")
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest" "-Dplaywright.headed=true"
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

## 4. Page Object Model (POM) 架構規範與四大分頁適配

專案嚴格貫徹 **Page Object Model (POM)** 設計模式，杜絕測試程式碼中散落脆弱的 CSS 選擇器，將 UI 互動與業務斷言分層解耦：

```
src/test/java/com/tibame/e2e/pages/
├── BasePage.java       # 頂層抽象：封裝導航、LocalStorage 憑據存取、SweetAlert2 動畫等待
├── LoginPage.java      # 認證頁面：封裝登入/註冊表單輸入、Tab 切換、提交與錯誤提示檢驗
└── LedgerPage.java     # 記帳頁面：封裝四大分頁導航、錄入表單、自動流轉斷言、明細篩選與卡片數值提取
```

### 4.1 四大分頁適配與非同步可見性處理 (Async Visibility)

#### 痛點剖析：`v-show` 造成的元素隱藏與 30 秒超時
在四大分頁架構中，財務概覽分頁標註 `v-show="activeTab === 'analytics'"`。在預設的「01 記帳錄入」分頁下，該元素被 Vue 設為 `display: none`。
若直接呼叫讀取卡片數值，Playwright 的 `waitForSelector` 預設等待元素狀態為 `VISIBLE`，導致 30 秒超時崩潰：
```text
Timeout 30000ms exceeded: waiting for locator(".swiss-stat-expense .swiss-stat-value") to be visible
  locator resolved to hidden <div class="swiss-stat-value"> - NT$ 0.00</div>
```

#### POM 自癒適配對策
`LedgerPage.java` 實作自動切換與可見性自癒機制：
1. **分頁切換封裝 (`switchTab`)**：封裝 `.swiss-tab-btn:has-text('03')` 點擊與 `.swiss-tab-btn.active:has-text('03')` 樣式穩定等待。
2. **卡片提取自癒**：在 `getTotalExpenseText()`、`getTotalIncomeText()` 與 `getNetBalanceText()` 內部主動調用 `switchTab("analytics")`，確保元素可見後才提取數值。
3. **自動流轉斷言 (`submitSmartQuickInput` / `submitStructuredInput`)**：記帳成功後，主動驗證前端平滑流轉至 `02 交易明細` 分頁 (`waitForSelector(".swiss-tab-btn.active:has-text('02')")`)。

### 4.2 `LedgerPage.java` 核心方法契約

```java
public class LedgerPage extends BasePage {

    // === 1. 分頁導航 ===
    public LedgerPage switchTab(String tabName); // 支援 'entry'/'01', 'history'/'02', 'analytics'/'03', 'categories'/'04'

    // === 2. 分頁 01：記帳錄入 ===
    public LedgerPage submitSmartQuickInput(String text); // 自然語言記帳並斷言自動流轉至 02
    public LedgerPage submitStructuredInput(String type, double amount, String categoryName, String note); // 結構化表單記帳

    // === 3. 分頁 02：交易明細 ===
    public LedgerPage assertRecordExists(String description); // 斷言流水帳行存在
    public LedgerPage toggleFilterPanel(); // 展開/收合多維度篩選器
    public LedgerPage filterByKeyword(String keyword); // 關鍵字快速過濾

    // === 4. 分頁 03：財務概覽 (自動切換至分頁 03) ===
    public String getTotalExpenseText(); // 當月總支出
    public String getTotalIncomeText();  // 當月總收入
    public String getNetBalanceText();   // 當月淨結餘

    // === 5. 分頁 04：分類管理 ===
    public LedgerPage createCategory(String type, String name, String iconCode); // 新增自訂收支分類

    // === 6. 身分操作 ===
    public LoginPage logout(); // 登出並返回 LoginPage
}
```

---

## 5. 全鏈路 13 大 E2E 測試案例矩陣盤點 (E2E Test Catalog Matrix v2.0)

專案具備完整覆蓋 API 整合、多租戶安全防護、真機 UI 認證與業務流轉的 13 大測試案例矩陣（10 個現行驗收案例 + 3 個擴充驗收案例）：

| # | 測試類別名稱 | 測試層級 | 測試方法與情境說明 | 核心檢驗斷言目標 | 狀態 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `AuthApiE2ETest` | API 整合 | `testRegisterAndLoginSuccess()`<br>測試使用者註冊與登入成功全流程 | 註冊回傳 HTTP 201，登入回傳 HTTP 200 並成功簽發有效 JWT 格式 | 現行 |
| **2** | `AuthApiE2ETest` | API 整合 | `testLoginFailureWrongPassword()`<br>測試錯誤密碼登入拒絕情境 | 密碼不正確時回傳 HTTP 401 Unauthorized，驗簽攔截生效 | 現行 |
| **3** | `AuthApiE2ETest` | API 整合 | `testRegisterDuplicateUsername()`<br>測試帳號重複註冊防護 | 重複 username 嘗試註冊時回傳 HTTP 409 Conflict，唯一鍵防護生效 | 現行 |
| **4** | `AuthApiE2ETest` | API 整合 | `testAccessProtectedEndpointWithoutToken()`<br>測試未授權存取保護端點 | 未攜帶 Bearer Token 存取 `/api/v1/records` 時回傳 HTTP 401 或 403 拒絕 | 現行 |
| **5** | `LedgerApiE2ETest` | API 整合 | `testRecordCrudFlow()`<br>測試流水帳記錄完整 CRUD 生命週期 | 驗證新增記帳、讀取單筆、更新記錄內容、物理刪除後查詢回傳 404 | 現行 |
| **6** | `LedgerApiE2ETest` | API 整合 | `testLedgerSummaryCalculation()`<br>測試月度收支聚合統計運算 | 驗證新增多筆收入與支出後，Summary API 準確聚合計算總收入、總支出與淨結餘 | 現行 |
| **7** | `TenantIsolationSecurityE2ETest` | API 安全 | `testCrossTenantRecordAccessBlocked()`<br>測試跨租戶流水帳水平越權讀取 | 使用者 A 嘗試以 IDOR 查詢使用者 B 之記錄時，必須回傳 HTTP 404 或 403 嚴格攔截 | 現行 |
| **8** | `TenantIsolationSecurityE2ETest` | API 安全 | `testCrossTenantCategoryModificationBlocked()`<br>測試跨租戶自訂分類竄改與刪除 | 使用者 A 嘗試修改或刪除使用者 B 的自訂分類時，必須回傳 HTTP 403 Forbidden 堅決拒絕 | 現行 |
| **9** | `AuthFlowUiE2ETest` | UI 真機 | `testAuthAndRedirectFlow()`<br>驗證註冊、自動登入、跳轉 /ledger 與 Token 持久化 | 驗證自動跳轉 `/ledger` 且 LocalStorage 存有 Token，點擊登出後 Token 被清除，再次登入成功 | 現行 |
| **10** | `AccountingFlowUiE2ETest` | UI 真機 | `testAccountingFlow()`<br>驗證智慧記帳、自動流轉分頁 02、表格渲染與分頁 03 卡片動態更新 | 在分頁 01 輸入自然語言快捷記帳、捕捉 Toast 提示、自動流轉分頁 02 渲染明細、分頁 03 統計卡片即時更新 | 現行 |
| **11** | `StructuredEntryUiTest` | UI 擴充 | 驗證結構化記帳錄入、金額聚焦與欄位校驗 | 驗證聚焦狀態、收支切換、分類下拉選單、表單驗證與入帳後自動流轉 | 擴充 |
| **12** | `LedgerFilterUiTest` | UI 擴充 | 驗證交易明細多維篩選、重設與分頁切換 | 驗證收支類型篩選、分類篩選、起訖日期篩選、備註搜尋與分頁翻頁 | 擴充 |
| **13** | `CategoryLifecycleUiTest` | UI 擴充 | 驗證分頁 04 自訂分類新增/刪除與系統唯讀保護 | 驗證在分頁 04 建立自訂分類、入帳使用該分類、刪除分類與系統內建分類防刪保護 | 擴充 |

---

## 6. 實務常見問題排查指南 (FAQ & Troubleshooting v2.0)

### Q1: 在 Windows PowerShell 執行指定測試時出現 `Unknown lifecycle phase` 錯誤？
- **原因**：PowerShell 會對命令列引數中的 `-Dit.test` 進行參數解析與截斷，導致 Maven 將其誤認為生命週期階段。
- **排查對策**：一律以雙引號括起參數，例如：
  ```powershell
  .\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest"
  ```

### Q2: 執行 UI 測試時出現 `Timeout 30000ms exceeded: waiting for locator(".swiss-stat-...") to be visible`？
- **原因**：工作台升級為四大分頁後，統計卡片位於 `03 財務概覽` 分頁容器內（`v-show="activeTab === 'analytics'"`）。在預設的 `01 記帳錄入` 分頁下，該元素被設為 `display: none`。Playwright 的 `waitForSelector` 預設要求元素必須 Visible，導致超時。
- **排查對策**：在提取卡片前，必須先調用 `switchTab("analytics")` 確保分頁切換至可見狀態（`LedgerPage` 目前已內建自癒機制）。

### Q3: 記帳送出後找不到輸入框或流水帳表格未在當前頁面刷新？
- **原因**：新版介面實作了「極速捕捉第一」的自動流轉機制（Auto Transition），記帳成功後前端 Vue 自動切換至 `activeTab = 'history'`。
- **排查對策**：測試代碼應順應此流轉行為，斷言當前啟用標籤切換至「02 交易明細」，並直接在分頁 02 驗證表格記錄。

### Q4: 首次執行時下載 Chromium 失敗或逾時？
- **原因**：Playwright Java 首次執行需下載 Chromium 二進位檔（約 150MB）。若處於受限企業網路可能中斷。
- **排查對策**：
  1. 快取路徑為 `%LOCALAPPDATA%\ms-playwright`（Windows）或 `~/.cache/ms-playwright`（Linux/macOS）。
  2. 若具備外部代理，可配置環境變數 `HTTPS_PROXY` 後重試。下載完成後永久快取。

### Q5: 為什麼執行 `.\mvnw.cmd test` 沒有執行任何 E2E 測試？
- **原因**：這是 Maven Surefire 的架構分流設計，排除了 `**/*IT.java` 與 `**/*E2ETest.java`，確保日常存檔編譯時單元測試能在 2~5 秒內完成。
- **排查對策**：若要執行 E2E 測試，請執行 `.\mvnw.cmd verify` 或 `.\mvnw.cmd test-compile failsafe:integration-test`。

### Q6: 測試執行中出現 409 Conflict 使用者名稱重複錯誤？
- **原因**：多個測試案例使用固定寫死的帳號註冊。
- **排查對策**：所有測試一律透過 `TestUserFactory.createRandomUser()` 動態產生隨機 UUID 帳號，確保 100% 冪等隔離。

### Q7: 隨機埠是否可能發生碰撞？
- **對策**：測試統一使用 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`，由作業系統核心動態指派閒置 Port，並透過 `@LocalServerPort` 注入，完全杜絕 8080 碰撞。

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
| [ ] 5. 分頁狀態意識: 查詢不同分頁元素前，必須調用 switchTab 切換至該分頁確保可見   |
| [ ] 6. 等待防禦原則: 嚴禁使用 Thread.sleep()，一律使用 Playwright 原生自動等待機制  |
| [ ] 7. 動態有頭相容: 繼承 PlaywrightTestBase，確保支援 -Dplaywright.headed 視覺除錯|
| [ ] 8. 繁體中文命名: 測試方法一律使用清晰的 @DisplayName 標明業務行為與預期結果    |
| [ ] 9. 本地全量綠燈: 送出 PR 前在本機執行 .\mvnw.cmd verify，確認零回歸、零失敗   |
+-----------------------------------------------------------------------------------+
```
