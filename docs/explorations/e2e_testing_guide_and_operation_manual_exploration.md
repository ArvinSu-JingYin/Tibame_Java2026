# 每日流水帳系統 (Daily Ledger System) — E2E 測試操作手冊、有頭除錯模式與案例盤點探索報告

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-03  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / Playwright Java 1.46+ / Maven Failsafe / Chromium / H2 In-Memory  
> **目標範疇**：CLI 雙軌測試分流、動態有頭模式 (Headed Mode)、Page Object Model 擴充規範、全鏈路案例矩陣盤點與 FAQ 排錯指南  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與核心目標 (Background & Objectives)

本專案「日常流水帳系統（Daily Ledger System）」目前已建置完整的自動化測試金字塔體系，包含：
1. **底層純單元測試**：8 個測試類別、共 54 個單元測試案例（由 JUnit 5 與 Mockito 支援，毫秒級極速執行）。
2. **中層 API 整合測試**：使用 `TestRestTemplate` 於隨機埠真實驗證 HTTP 安全鏈路、JWT 驗簽、業務 CRUD 與跨租戶橫向越權防護 (IDOR)。
3. **頂層 UI 真機 E2E 測試**：採用 **Playwright Java** 驅動 Chromium 瀏覽器，透過 Page Object Model (POM) 完整驗收使用者註冊登入、智慧自然語言記帳、SweetAlert2 彈窗反饋與儀表板即時刷新。

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

### 探索核心目標
本報告旨在將本次探索所得之測試執行維運思維沉澱為可落地的規範，聚焦於：
1. **解除 CLI 執行認知障礙**：釐清 Surefire 與 Failsafe 的 Maven 雙軌分流機制，提供隨開即用的指令清單。
2. **突破除錯黑盒**：設計並推演**「動態有頭模式 (Headed Mode)」**，讓工程師能在本機除錯時直觀觀察瀏覽器操作。
3. **沉澱 POM 開發範式**：定義清晰的元素選取原則與非同步等待機制，嚴格杜絕脆落的 `Thread.sleep`。
4. **盤點現存測試資產**：完整盤點目前 5 大 E2E 測試類別的測試情境與檢驗目標。
5. **奠定規格化基礎**：為後續建立 `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md` 提供完整藍圖。

---

## 2. Maven 雙軌分流機制與 CLI 操作速查 (Maven Surefire vs Failsafe)

為了保障開發者在日常存檔與編譯時獲得即時反饋，本專案在 `pom.xml` 中將測試嚴格分為兩條管線：

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

### CLI 命令列速查清單 (PowerShell)

```powershell
# 1. 執行全專案全套驗證 (包含單元測試 + API 整合測試 + UI 真機測試)
.\mvnw.cmd verify

# 2. 僅執行整合與 E2E 測試 (跳過單元測試重複編譯執行，加速除錯)
.\mvnw.cmd test-compile failsafe:integration-test

# 3. 執行指定的 E2E 測試類別 (使用 -Dit.test 參數)
# 執行核心記帳 UI 測試
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AccountingFlowUiE2ETest

# 執行身分認證 UI 測試
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AuthFlowUiE2ETest

# 執行跨租戶越權防護 API 測試
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=TenantIsolationSecurityE2ETest

# 4. 僅執行特定測試方法 (語法：類別名#方法名)
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AccountingFlowUiE2ETest#testAccountingFlow

# 5. 使用萬用字元批次執行測試群組
# 執行所有 UI 測試
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=*UiE2ETest

# 執行所有 API 整合測試
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=*ApiE2ETest

# 6. 開啟詳細 Debug 日誌
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AccountingFlowUiE2ETest -X
```

---

## 3. 動態有頭模式 (Dynamic Headed Mode) 與視覺化除錯設計

### 3.1 探索痛點
Playwright 預設使用無頭模式 (`headless: true`)，這對 CI/CD 與自動化構建是最佳實踐。但在本機除錯前端互動、CSS 動畫或彈窗定位時，開發者看不到瀏覽器畫面，排查問題非常依賴日誌推敲。

### 3.2 參數化有頭模式架構推演
為達成「隨開即看、日常極速無干擾」的目標，我們推演在 `PlaywrightTestBase.java` 中引入參數化動態偵測機制：

```java
// 核心推演設計：支援 JVM 系統屬性與環境變數雙軌偵測
boolean isHeaded = Boolean.parseBoolean(System.getProperty("playwright.headed", "false"))
        || "true".equalsIgnoreCase(System.getenv("PLAYWRIGHT_HEADED"));

browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions()
                .setHeadless(!isHeaded)
                .setSlowMo(isHeaded ? 400 : 0) // 有頭模式自動加入 400ms 微延遲，便於肉眼跟隨操作
);
```

### 3.3 開啟有頭模式指令
工程師除錯時，只需在命令列附加 `-Dplaywright.headed=true`：
```powershell
# 以可見瀏覽器畫面運行記帳 E2E 測試，並放慢動作便於觀察
.\mvnw.cmd test-compile failsafe:integration-test -Dit.test=AccountingFlowUiE2ETest -Dplaywright.headed=true
```

### 3.4 IDE 單鍵除錯技巧
- **IntelliJ IDEA / VS Code / Antigravity IDE**：
  - 開啟任一 `*E2ETest.java` 檔案，在方法名稱左側點擊綠色執行箭頭。
  - 若需觀察畫面，可在 Run/Debug Configuration 中的 VM Options 填入 `-Dplaywright.headed=true`。
  - 可在中斷點直接下在 Controller、Service 或 Page Object 內部，測試執行至該行時會暫停，此時瀏覽器畫面亦會凍結在當前操作狀態。

---

## 4. Page Object Model (POM) 架構規範與非同步處理

專案採用嚴謹的 Page Object Model，將 DOM 樹細節與業務語意分離：

```
src/test/java/com/tibame/e2e/pages/
├── BasePage.java       # 封裝基礎導航、LocalStorage Token 讀取、SweetAlert2 彈窗偵測
├── LoginPage.java      # 封裝 /login 頁面切換、表單輸入、提交與錯誤斷言
└── LedgerPage.java     # 封裝 /ledger 快速記帳、表格即時查詢、統計卡片數值提取、安全登出
```

### 4.1 核心設計原則
1. **Auto-waiting 原生等待（嚴禁 `Thread.sleep`）**：
   - 傳統 Selenium 常依賴 `Thread.sleep(2000)`，極度脆弱且拖慢測試。
   - Playwright 原生具備自動等待機制，在執行 `click`、`fill` 前會自動確認元素是否為 Actionable（可見、啟用、可交互）。
2. **非同步彈窗捕捉封裝 (`BasePage`)**：
   - SweetAlert2 彈窗具備淡入淡出非同步動畫，`BasePage` 封裝專屬等待邏輯：
     ```java
     public void waitForSweetAlert(double timeoutMs) {
         page.waitForSelector(".swal2-container, .swal2-popup, .swal2-toast", 
                 new Page.WaitForSelectorOptions()
                     .setState(WaitForSelectorState.VISIBLE)
                     .setTimeout(timeoutMs));
     }
     ```
3. **LocalStorage 憑據自檢**：
   - 不僅驗證 URL 跳轉，更透過 `page.evaluate("() => localStorage.getItem('jwt_token')")` 直接檢驗前端認證狀態機是否正確持久化或清除憑證。

---

## 5. 現有 E2E 測試案例矩陣盤點 (E2E Test Catalog & Matrix)

專案目前具備 5 個核心 E2E 測試類別，涵蓋後端全鏈路與前端真機驗證：

```
+-------------------------------------------------------------------------------------------------------------------+
|                                            全專案 E2E 測試案例矩陣盤點                                             |
+---+----------------------------------+------+----------------------------------------+----------------------------+
| # | 測試類別名稱                     | 類型 | 測試案例說明                           | 核心檢驗斷言目標           |
+---+----------------------------------+------+----------------------------------------+----------------------------+
| 1 | AuthApiE2ETest                   | API  | 測試使用者註冊與登入成功全流程         | HTTP 201/200, 回傳 JWT     |
| 2 | AuthApiE2ETest                   | API  | 測試密碼不正確登入拒絕情境             | HTTP 401 Unauthorized      |
| 3 | AuthApiE2ETest                   | API  | 測試帳號重複註冊防護                   | HTTP 409 Conflict          |
| 4 | AuthApiE2ETest                   | API  | 測試未帶 Token 存取受保護端點          | HTTP 403 / 401 拒絕        |
| 5 | LedgerApiE2ETest                 | API  | 測試記帳流水帳新增、讀取、更新、刪除   | 完整生命週期 CRUD 狀態機   |
| 6 | LedgerApiE2ETest                 | API  | 測試月度收支統計聚合運算               | 總收入、總支出與淨結餘校驗 |
| 7 | TenantIsolationSecurityE2ETest   | API  | 測試多租戶橫向越權 (IDOR) 讀取攔截     | HTTP 404 / 403 嚴格隔離    |
| 8 | TenantIsolationSecurityE2ETest   | API  | 測試跨租戶分類與記帳竄改/刪除攔截      | HTTP 403 Forbidden 拒絕    |
| 9 | AuthFlowUiE2ETest                | UI   | 驗證註冊、登入、跳轉 /ledger 與 Token  | 路由跳轉、LocalStorage 驗證|
| 10| AccountingFlowUiE2ETest          | UI   | 驗證智慧自然語言記帳、表格刷新與登出   | 語意解析、Swal2、卡片即時算|
+---+----------------------------------+------+----------------------------------------+----------------------------+
```

---

## 6. 實務常見問題排查指南 (FAQ & Troubleshooting)

### Q1: 首次執行時下載 Chromium 失敗或逾時？
- **原因**：Playwright Java 首次執行時需自微軟官方伺服器下載 Chromium 二進位檔（約 150MB）。若在受限制的企業網路或離線環境，可能觸發逾時中斷。
- **排查對策**：
  1. 驅動預設存放於本機目錄：`%LOCALAPPDATA%\ms-playwright`（Windows）或 `~/.cache/ms-playwright`（Linux/macOS）。
  2. 若環境具備外部代理，可配置環境變數 `HTTPS_PROXY` 後重試。
  3. 驅動下載成功一次後將永久快取，後續所有測試皆在幾百毫秒內喚醒。

### Q2: 為什麼打 `.\mvnw.cmd test` 沒有看到任何 E2E 測試被執行？
- **原因**：這是架構設計的刻意分流！Surefire 插件排除了 `**/*IT.java` 與 `**/*E2ETest.java`，確保日常單元測試維持毫秒級反饋。
- **排查對策**：若要執行 E2E 測試，請使用 `.\mvnw.cmd verify` 或 `.\mvnw.cmd test-compile failsafe:integration-test`。

### Q3: 測試執行中出現 409 Conflict 使用者名稱重複錯誤？
- **原因**：多個測試案例使用硬編碼之固定帳號（如 `testuser`）向資料庫註冊。
- **排查對策**：所有測試一律透過 `TestUserFactory.createRandomUser()` 動態生成帶有隨機 UUID 尾碼的帳號，確保測試間 100% 冪等且互不干擾。

### Q4: 隨機埠是否可能發生碰撞？
- **對策**：測試統一使用 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`，由作業系統核心動態分配未被佔用的 Port，並透過 `@LocalServerPort` 注入，完全杜絕 8080 碰撞。

---

## 7. 規格化落地建議與後續路線圖 (Next Steps & Roadmap)

本探索報告已完整釐清 E2E 測試的操作體系、有頭除錯開關與案例盤點。後續建議執行以下規格化行動：

```
+-----------------------------------------------------------------------------------+
|                            規格化與代碼落地路線圖                                 |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   1. 發起 OpenSpec 變更：add-e2e-testing-guide                                    |
|          |                                                                        |
|          +--> 代碼實作：於 PlaywrightTestBase.java 補上動態有頭模式開關與 SlowMo  |
|          |                                                                        |
|          +--> 正式手冊：於 docs/specifications/daily_ledger_system/ 建立          |
|          |    10_e2e_testing_guide_and_operation_manual.md                        |
|          |                                                                        |
|          +--> 索引同步：更新 docs/specifications/daily_ledger_system/README.md    |
|          |    與 docs/README.md 門戶                                              |
|          |                                                                        |
|          +--> 驗收封存：執行 .\mvnw.cmd verify 驗證全綠燈後封存變更               |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```
