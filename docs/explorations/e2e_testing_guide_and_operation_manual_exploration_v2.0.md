# 每日流水帳系統 (Daily Ledger System) — E2E 測試操作手冊、四大分頁工作台適配與案例盤點探索報告 (v2.0)

> **文件版本**：v2.0.0  
> **建立日期**：2026-09-05  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **前版參照**：[v1.0 歷史探索報告 (e2e_testing_guide_and_operation_manual_exploration_v1.0.md)](e2e_testing_guide_and_operation_manual_exploration_v1.0.md)  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / Playwright Java 1.46+ / Maven Failsafe / Chromium / H2 In-Memory / Vue 3 MVVM / Swiss Style  
> **目標範疇**：四大分頁工作台 POM 適配、Playwright 非同步可見性排錯、Windows PowerShell CLI 雙引號規範、動態有頭模式 (Headed Mode) 與 13 大全鏈路案例矩陣  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與工作台架構演進 (Background & Workbench Evolution)

本專案「日常流水帳系統（Daily Ledger System）」目前已建置完整的自動化測試金字塔體系，由底層至頂層分為三道防線：
1. **底層純單元測試**：8 個測試類別、共 54 個單元測試案例（由 JUnit 5 與 Mockito 支援，毫秒級極速執行）。
2. **中層 API 整合測試**：使用 `TestRestTemplate` 於隨機埠真實驗證 HTTP 安全鏈路、JWT 驗簽、業務 CRUD 與跨租戶橫向越權防護 (IDOR)。
3. **頂層 UI 真機 E2E 測試**：採用 **Playwright Java** 驅動 Chromium 瀏覽器，透過 Page Object Model (POM) 完整驗收使用者註冊登入、智慧自然語言記帳、SweetAlert2 彈窗反饋與儀表板即時刷新。

```
+---------------------------------------------------------------------------------------------------+
|                                專案測試金字塔與沙盒隔離執行架構                                     |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   [頂層 UI 真機 E2E]       Playwright Chromium + POM 封裝                                         |
|   (真機端到端驗證)         - AuthFlowUiE2ETest (註冊/登入/LocalStorage Token 檢驗/登出)            |
|                            - AccountingFlowUiE2ETest (智慧記帳/自動流轉/明細渲染/統計更新)         |
|                            - [規劃] StructuredEntry / LedgerHistory / CategoryMgmt 等專注驗收     |
|                                     ^                                                             |
|   [中層 API 全鏈路 E2E]    Spring Boot (RANDOM_PORT) + TestRestTemplate                           |
|   (整合安全鏈路驗證)       - TenantIsolationSecurityE2ETest (跨租戶越權攔截 403/404)              |
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

### 1.1 工作台從「單頁堆疊」演進為「四大分頁」
在專案最新的介面重構中，前端畫面由原先的「單頁垂直堆疊佈局」全面升級為**瑞士風格四大分頁式工作台 (Tabbed Ledger Workbench)**：

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
|   - 雙次模式切換：結構化錄入 vs 自然語言解析 (NLP)                                                |
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

## 2. Maven 雙軌分流生命週期與 CLI 操作速查

專案在 `pom.xml` 中將日常存檔測試與全鏈路整合測試嚴格分流：

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
|          |    - 喚醒 Playwright Chromium 無頭瀏覽器進程 (免 Node.js)               |
|          |    - 依序執行 *IT.java 與 *E2ETest.java                                |
|          |                                                                        |
|          +--> phase: verify (由 Failsafe 彙整整合測試驗收結果)                     |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### 2.1 【重大避坑指南】Windows PowerShell 命令列參數雙引號規範
在 Windows PowerShell 環境下，Maven 參數若包含 `-D` 前綴，PowerShell 可能將其拆解導致命令解析失敗：
- **錯誤示範**：`.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AccountingFlowUiE2ETest`  
  -> 會報錯 `[ERROR] Unknown lifecycle phase ".test=AccountingFlowUiE2ETest"`！
- **正確規範**：所有 `-D` 參數必須一律使用**雙引號括起**！

### 2.2 CLI 命令列速查清單 (PowerShell 規範格式)

```powershell
# 1. 執行全專案全套驗證 (單元測試 + API 整合測試 + UI 真機 E2E 測試)
.\mvnw.cmd verify

# 2. 僅編譯並執行整合與 E2E 測試 (跳過單元測試重複執行，加速除錯)
.\mvnw.cmd test-compile failsafe:integration-test

# 3. 執行指定之 E2E 測試類別 (使用雙引號括起 -Dit.test 參數)
# 執行核心記帳 UI 測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest"

# 執行身分認證 UI 測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AuthFlowUiE2ETest"

# 執行跨租戶越權防護 API 測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=TenantIsolationSecurityE2ETest"

# 4. 僅執行特定測試方法 (語法：類別名#方法名)
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest#testAccountingFlow"

# 5. 使用萬用字元批次執行測試群組
# 執行所有 UI 真機測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=*UiE2ETest"

# 執行所有 API 整合測試
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=*ApiE2ETest"

# 6. 開啟動態有頭模式 (Headed Mode) 觀察畫面操作 (附加 -Dplaywright.headed=true)
.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest" "-Dplaywright.headed=true"
```

---

## 3. 動態有頭模式 (Dynamic Headed Mode) 與視覺化除錯

### 3.1 實作架構現況
專案基底類別 `PlaywrightTestBase.java` 目前已正式實作雙軌動態有頭開關：

```java
// 支援 JVM 系統屬性與環境變數雙軌偵測
boolean isHeaded = Boolean.parseBoolean(System.getProperty("playwright.headed", "false"))
        || Boolean.parseBoolean(System.getenv("PLAYWRIGHT_HEADED"));

BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
        .setHeadless(!isHeaded);

if (isHeaded) {
    launchOptions.setSlowMo(400); // 有頭模式自動加入 400ms 微延遲，便於工程師肉眼跟隨畫面互動
}

browser = playwright.chromium().launch(launchOptions);
```

### 3.2 視覺化除錯實務技巧
1. **指令啟用**：加上 `"-Dplaywright.headed=true"` 即可即時喚起本機 Chromium 視窗。
2. **IDE 斷點凍結畫面**：在 Controller、Service 或 Page Object 內部設置斷點，當測試執行暫停時，Chromium 瀏覽器畫面將保持凍結在當前 DOM 狀態，可直觀開啟 DevTools 檢查元素與 Vue 狀態。

---

## 4. Page Object Model (POM) 架構規範與四大分頁適配

### 4.1 POM 目錄職責拓撲
```
src/test/java/com/tibame/e2e/
├── base/
│   ├── PlaywrightTestBase.java   # Spring Boot 隨機埠管理、Chromium 生命週期、動態有頭開關
│   └── TestUserFactory.java      # 動態 UUID 隨機帳號工廠，杜絕 409 Conflict
├── pages/
│   ├── BasePage.java             # 基礎導航、LocalStorage Token 讀取、SweetAlert2 捕捉
│   ├── LoginPage.java            # /login 頁面表單操作、登入/註冊 Tab 切換與錯誤提示斷言
│   └── LedgerPage.java           # /ledger 四大分頁導航、錄入表單、明細篩選與卡片數值提取
├── ui/
│   ├── AuthFlowUiE2ETest.java    # 身分認證與導航真機驗收
│   └── AccountingFlowUiE2ETest.java # 記帳黃金路徑真機驗收
└── api/
    ├── AuthApiE2ETest.java       # 身分認證 HTTP API 鏈路測試
    ├── LedgerApiE2ETest.java     # 記帳 CRUD 與統計聚合 API 測試
    └── TenantIsolationSecurityE2ETest.java # 多租戶 IDOR 安全防護 API 測試
```

### 4.2 【實機報錯深入剖析】Playwright 等待隱藏元素超時問題
在分頁重構後，直接執行舊版 `LedgerPage.getTotalExpenseText()` 會觸發以下超時中斷：
```text
waiting for locator(".swiss-stat-expense .swiss-stat-value") to be visible
  locator resolved to hidden <div class="swiss-stat-value"> - NT$ 0.00</div>
```
- **核心根因**：
  進入 `/ledger` 預設停留在 `01 記帳錄入` 分頁，此時 `03 財務概覽` 分頁容器標註 `v-show="activeTab === 'analytics'"`，Vue 將其設為 `display: none;`。
  Playwright 的 `page.waitForSelector(...)` 預設等待元素狀態為 `VISIBLE`。由於元素處於 hidden 狀態，等待達到 30 秒上限引發 `TimeoutError`。
- **POM 適配規範**：
  `LedgerPage` 必須封裝四大分頁切換邏輯，讀取財務卡片前**必須先切換至「03 財務概覽」分頁**！

### 4.3 LedgerPage 擴充重構設計契約
配合四大分頁，`LedgerPage` 應具備以下方法契約：

```java
public class LedgerPage extends BasePage {

    // === 1. 分頁切換與狀態斷言 ===
    public LedgerPage switchTab(String tabName) { // 'entry' | 'history' | 'analytics' | 'categories'
        page.click(".swiss-tab-btn:has-text('" + getTabButtonLabel(tabName) + "')");
        page.waitForSelector(".swiss-tab-btn.active:has-text('" + getTabButtonLabel(tabName) + "')");
        return this;
    }

    // === 2. 分頁 01：記帳錄入 (Quick Entry) ===
    // 智慧自然語言快速記帳 (切換 NLP 子模式、輸入並送出，送出後自動流轉至分頁 02)
    public LedgerPage submitSmartQuickInput(String text) {
        switchTab("entry");
        page.click("button:has-text('自然語言解析')");
        page.fill(".swiss-amount-input[placeholder*='例如：午餐便當']", text);
        page.click("button:has-text('智能解析入帳')");
        // 自動流轉斷言：等待切換至 02 交易明細
        page.waitForSelector(".swiss-tab-btn.active:has-text('交易明細')");
        return this;
    }

    // 結構化表單記帳
    public LedgerPage submitStructuredInput(String type, double amount, String categoryName, String note) {
        switchTab("entry");
        page.click("button:has-text('結構化錄入')");
        page.click("button.type-toggle-btn:has-text('" + (type.equals("EXPENSE") ? "支出" : "收入") + "')");
        page.fill("#quickAmountInput", String.valueOf(amount));
        page.selectOption("select.form-select-swiss", new SelectOption().setLabel(categoryName));
        page.fill("input[placeholder*='請輸入備註說明']", note);
        page.click("button[type='submit']:has-text('入帳送出')");
        page.waitForSelector(".swiss-tab-btn.active:has-text('交易明細')");
        return this;
    }

    // === 3. 分頁 02：交易明細 (Ledger History) ===
    public LedgerPage assertRecordExists(String description) {
        page.waitForSelector("table.table-swiss tbody tr:has-text('" + description + "')");
        return this;
    }

    // === 4. 分頁 03：財務概覽 (Financial Analytics) ===
    public String getTotalExpenseText() {
        switchTab("analytics"); // 先切換至分頁 03，確保元素可見！
        page.waitForSelector(".swiss-stat-expense .swiss-stat-value");
        return page.textContent(".swiss-stat-expense .swiss-stat-value").trim();
    }

    // === 5. 分頁 04：分類管理 (Category Management) ===
    public LedgerPage createCategory(String type, String name, String icon) {
        switchTab("categories");
        // 填寫分類名稱並送出...
        return this;
    }
}
```

---

## 5. 全鏈路測試案例矩陣盤點 (E2E Test Catalog & Matrix v2.0)

本專案現有與擴充之 13 大核心測試案例矩陣如下：

```
+-------------------------------------------------------------------------------------------------------------------+
|                                      全專案 E2E 測試案例矩陣盤點 (v2.0)                                             |
+----+--------------------------------+------+----------------------------------------+-----------------------------+
| #  | 測試類別 / 案例名稱            | 類型 | 測試案例說明                           | 核心檢驗斷言目標            |
+----+--------------------------------+------+----------------------------------------+-----------------------------+
| 01 | AuthApiE2ETest                 | API  | 測試使用者註冊與登入成功全流程         | HTTP 201/200, 回傳 JWT      |
| 02 | AuthApiE2ETest                 | API  | 測試密碼不正確登入拒絕情境             | HTTP 401 Unauthorized       |
| 03 | AuthApiE2ETest                 | API  | 測試帳號重複註冊防護                   | HTTP 409 Conflict           |
| 04 | AuthApiE2ETest                 | API  | 測試未帶 Token 存取受保護端點          | HTTP 403 / 401 拒絕         |
| 05 | LedgerApiE2ETest               | API  | 測試記帳流水帳新增、讀取、更新、刪除   | 完整生命週期 CRUD 狀態機    |
| 06 | LedgerApiE2ETest               | API  | 測試月度收支統計聚合運算               | 總收入、總支出與淨結餘校驗  |
| 07 | TenantIsolationSecurityE2ETest | API  | 測試多租戶橫向越權 (IDOR) 讀取攔截     | HTTP 404 / 403 嚴格隔離     |
| 08 | TenantIsolationSecurityE2ETest | API  | 測試跨租戶分類與記帳竄改/刪除攔截      | HTTP 403 Forbidden 拒絕     |
| 09 | AuthFlowUiE2ETest              | UI   | 驗證註冊、登入、跳轉 /ledger 與 Token  | 路由跳轉、LocalStorage 憑證 |
| 10 | AccountingFlowUiE2ETest        | UI   | 驗證智慧 NLP 記帳 -> 自動流轉分頁 02   | Toast 捕捉、自動分頁切換、  |
|    |                                |      | -> 明細列表渲染 -> 分頁 03 財務卡片更新| 表格渲染與統計卡片動態運算  |
| 11 | [擴充] StructuredEntryUiTest   | UI   | 驗證結構化記帳錄入、金額聚焦與欄位校驗 | 聚焦狀態、表單驗證、自動流轉|
| 12 | [擴充] LedgerFilterUiTest      | UI   | 驗證交易明細多維篩選、重設與分頁切換   | 即時過濾、清空重設、分頁翻頁|
| 13 | [擴充] CategoryLifecycleUiTest | UI   | 驗證分頁 04 自訂分類新增/刪除與唯讀保護| 自訂分類入帳、系統保護防刪  |
+----+--------------------------------+------+----------------------------------------+-----------------------------+
```

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
- **排查對策**：在提取卡片前，必須先調用 `switchTab("analytics")` 確保分頁切換至可見狀態。

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

## 7. 規格化落地與代碼修正路線圖 (Next Steps & Roadmap)

本探索報告已完整釐清四大分頁工作台對 E2E 測試線束的影響與修復方案。建議後續採取以下行動：

```
+-----------------------------------------------------------------------------------+
|                            規格化與代碼修復路線圖                                 |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   1. 歸檔與建立探索手冊：已建立 v1.0 與 v2.0 探索報告                             |
|          |                                                                        |
|          v                                                                        |
|   2. 發起 OpenSpec 變更提案：adapt-e2e-tests-for-tabbed-workbench                 |
|          |                                                                        |
|          +--> 代碼重構：升級 LedgerPage.java 支援四大分頁切換與非同步可見性處理    |
|          |                                                                        |
|          +--> 測試修復：更新 AccountingFlowUiE2ETest.java 適應自動流轉與卡片提取   |
|          |                                                                        |
|          +--> 綠燈驗證：執行 .\mvnw.cmd verify 確保單元、API 與真機 E2E 全部通過   |
|          |                                                                        |
|          +--> 正式手冊：更新 docs/specifications/daily_ledger_system/              |
|          |    10_e2e_testing_guide_and_operation_manual.md                        |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```
