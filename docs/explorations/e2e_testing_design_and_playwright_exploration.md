# 端到端測試體系設計與 Playwright Java 選型探索報告 (E2E Testing Design & Playwright Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-03  
> **文件軌道**：探索文件軌 (Exploration Track)  
> **技術棧**：Java 21 / Spring Boot 3.3.3 / Playwright Java / JUnit 5 / H2 In-Memory DB  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **關聯規範**：[單元測試設計規格 (core_services_unit_testing_design.md)](../specifications/core_services_unit_testing_design.md)、[通用工程標準 (engineering_standards_and_code_cleanliness.md)](../specifications/engineering_standards_and_code_cleanliness.md)

---

## 1. 概述與探索背景 (Overview & Objectives)

隨著「日常流水帳系統 (Daily Ledger System)」後端服務（Spring Boot 3.3）與前端界面（Thymeleaf + 離線 Vue 3 / Axios / SweetAlert2）的成熟，現有體系已具備以 Mockito 為主的純單元測試。然而，單元測試無法覆蓋以下關鍵真實鏈路：

1. **真實 HTTP / Filter 鏈路**：Spring Security 結合 JWT Bearer Token 的授權攔截與過期重導向。
2. **多租戶資料穿透防護**：使用者 A 嘗試透過修改 URL 或 Request Body 竄改使用者 B 的記帳資料時，資料庫層與權限層的真實攔截。
3. **前端響應式與非同步連動**：Vue 3 與 Axios 在瀏覽器中的非同步拉取、SweetAlert2 彈窗互動，以及操作後儀表板數字的即時更新。

本探索旨在為專案定義一套**高穩定度、低維護成本、且完全維持純 Java 技術棧（Zero-Node.js）**的端到端（E2E, End-to-End）測試體系。

---

## 2. 測試金字塔分層架構 (Testing Pyramid Architecture)

為兼顧**執行速度**與**真實度保證**，專案採取「金字塔組合策略」，將測試劃分為三層：

```
+---------------------------------------------------------------------------------------------------+
|                              記帳系統測試金字塔 (Testing Pyramid)                                  |
+---------------------------------------------------------------------------------------------------+

                    / \
                   /   \
                  / UI  \        [頂層: 10% - 瀏覽器真機 E2E]
                 /  E2E  \       - 工具: Playwright Java (JUnit 5)
                /---------\      - 邊界: 真正啟動 Chromium，模擬使用者點擊、Vue3 渲染、SweetAlert2
               /           \     - 聚焦: 核心黃金路徑 (登入成功跳轉、記帳操作、即時餘額刷新)
              /   API E2E   \
             /  (Integration)\   [中層: 30% - API 全鏈路整合測試]
            /                 \  - 工具: @SpringBootTest(RANDOM_PORT) + TestRestTemplate
           /-------------------\ - 邊界: 穿透 Spring Security Filter -> Controller -> JPA -> H2
          /                     \- 聚焦: 完整業務狀態機、HTTP 狀態碼、多租戶越權攻擊攔截
         /       Unit Test       \
        /     (JUnit 5 + Mockito) \ [底層: 60% - 純單元測試] (現有已具備)
       /                           \- 聚焦: CategoryService / LedgerService / 正則解析邊界條件
      +-----------------------------+
```

### 分層職責與指標比對

| 測試層級 | 執行工具 | 執行耗時 | 覆蓋目標 | 執行時機 |
| :--- | :--- | :--- | :--- | :--- |
| **頂層：UI E2E** | Playwright Java + JUnit 5 | 秒級 (~5-15s) | 關鍵使用者黃金路徑、DOM 事件、LocalStorage Token、彈窗反饋 | CI/CD 建置、PR 審查 (`mvn verify`) |
| **中層：API E2E** | Spring Boot Test + TestRestTemplate | 百毫秒級 (~1-3s) | 完整 REST API 業務閉環、安全過濾器、資料庫事務、跨租戶防護 | 本地整合檢驗、CI/CD 建置 |
| **底層：Unit Test** | JUnit 5 + Mockito | 毫秒級 (<1s) | Service 業務分支、例外拋出、正規表達式解析、工具類 | 每次存檔或編譯 (`mvn test`) |

---

## 3. 執行環境與技術選型決策 (Architecture Decisions)

### 決策 1：選用 Playwright Java 而非 Node.js / Selenium
- **零 Node.js 依賴**：Playwright Java 透過 Java Process 自動管理微軟官方提供的 Playwright Driver 與 Chromium 瀏覽器，無須安裝 Node.js、npm 或維護 `package.json`。
- **Auto-waiting 原生防 Flaky 機制**：Playwright 會自動等待元素處於 Actionable 狀態（可點擊、可見、非禁用），完美契合 Vue 3 + Axios 的非同步時序，徹底告別脆弱的 `Thread.sleep`。
- **純 Java 工具鏈一致性**：測試代碼與後端專案共用相同的 IDE、語法高亮、除錯工具與 Maven 依賴管理。

### 決策 2：測試資料庫採用 H2 記憶體資料庫 (方案 A)
- **環境解耦**：使用專屬的 `src/test/resources/application-test.yml`，每次測試啟動獨立的 H2 In-memory DB。
- **初始種子隔離**：自動載入 `schema.sql` 與 `data.sql`（包含 10 大系統預設分類），測試結束容器關閉時自動銷毀，確保 100% 乾淨無殘留，不依賴外部 Docker 或實體 MSSQL。

```
+----------------------------------------------------------------------------------------------------+
|                       Playwright Java + H2 記憶體資料庫 執行時架構                                     |
+----------------------------------------------------------------------------------------------------+

   [測試啟動階段]
   ./mvnw.cmd verify
         |
         +--> [Spring Boot 容器] 
         |      - 載入 application-test.yml (Profile: test)
         |      - 啟動 H2 in-memory DB (自動載入 schema.sql + data.sql)
         |      - 監聽隨機 Port (例如 :54321)
         |
         +--> [Playwright Java 核心]
                - 自動從微軟快取拉起 Chromium 無頭瀏覽器
                - 建立 BrowserContext (無痕沙盒隔離，LocalStorage 互不干擾)

   [測試執行階段]
   
   +----------------------------------------------------------------------------------------------+
   | 1. API E2E (中層 - 快、精確)                                                                  |
   |    - 透過 TestRestTemplate 直接對 http://localhost:54321 發送 HTTP 請求                       |
   |    - 驗證 Auth 註冊/登入、Token 生成                                                           |
   |    - 驗證多租戶隔離 (橫向越權攻擊攔截: User B 竄改 User A 資料必定回傳 403/404)               |
   +----------------------------------------------------------------------------------------------+
                                                |
   +----------------------------------------------------------------------------------------------+
   | 2. UI E2E (頂層 - 真機體驗)                                                                  |
   |    - 啟動 Chromium 開啟 http://localhost:54321/login                                         |
   |    - 透過 Page Object (LoginPage) 輸入帳號密碼 -> 點擊登入                                     |
   |    - 斷言瀏覽器成功導航至 /ledger，且 LocalStorage 已存有 Bearer Token                         |
   |    - 透過 LedgerPage 操作「智慧快速記帳」輸入 "午餐 120"                                     |
   |    - 自動等待並斷言 SweetAlert2 成功視窗彈出                                                   |
   |    - 斷言下方記帳清單表格即時出現 "午餐"，上方統計卡片「本月支出」數字即時更新                |
   |    - 點擊「登出」-> 斷言 LocalStorage Token 清空且重導向回 /login                              |
   +----------------------------------------------------------------------------------------------+

   [測試清理階段]
   關閉 BrowserContext -> 關閉 Browser -> 銷毀 Spring 上下文與 H2 DB
```

---

## 4. 測試資料隔離與生命週期管理 (Data Isolation)

E2E 最常遇到的問題是資料衝突（例如使用者帳號重複）。本架構採取雙重隔離策略：

1. **動態隨機測試帳號 (`TestUserFactory`)**：
   - 每個測試案例執行時，動態生成帶 UUID 後綴的帳號：`test_user_${UUID.randomUUID().toString().substring(0,8)}`。
   - 確保併發或重複執行測試時，絕對不會觸發 `409 Conflict (帳號已存在)`。
2. **BrowserContext 無痕沙盒隔離**：
   - 在 JUnit 5 的 `@BeforeEach` 中建立全新 `BrowserContext`，在 `@AfterEach` 中關閉。
   - 瀏覽器的 Cookie、Session 與 `localStorage` 在案例之間完全隔離，避免前一個案例的登入態污染下一個案例。

---

## 5. Page Object Model (POM) 設計藍圖

為了防止前端 DOM 元素、CSS Class 或文字更動導致測試大面積損壞，採用 POM 模式進行物件導向封裝：

```
src/test/java/com/tibame/
  └── e2e/
       ├── base/
       │    ├── PlaywrightTestBase.java       <-- 注入隨機埠、管理 Playwright/BrowserContext 生命週期
       │    └── TestUserFactory.java          <-- 動態生成不衝突的測試帳號與密碼
       │
       ├── pages/                            <-- Page Object 封裝
       │    ├── BasePage.java                 <-- 封裝共用導航、SweetAlert2 捕捉、Token 檢查
       │    ├── LoginPage.java                <-- 封裝登入/註冊切換、表單輸入、錯誤訊息捕捉
       │    └── LedgerPage.java               <-- 封裝快速記帳輸入、分類管理 Modal、統計金額讀取
       │
       ├── api/                              <-- 中層 API E2E
       │    ├── AuthApiE2ETest.java           <-- 註冊、登入、Token 驗證
       │    ├── LedgerApiE2ETest.java         <-- 記帳 CRUD、月度統計計算
       │    └── TenantIsolationSecurityE2ETest.java <-- 橫向越權攻擊穿透防護
       │
       └── ui/                               <-- 頂層 UI E2E
            ├── AuthFlowUiE2ETest.java        <-- 註冊 -> 登入 -> 跳轉儀表板
            └── AccountingFlowUiE2ETest.java  <-- 快速記帳 -> 彈窗確認 -> 統計卡片刷新 -> 登出
```

### 核心 Page Object 職責示意

```java
// LoginPage: 專注於登入頁面之 DOM 封裝與動作
public class LoginPage extends BasePage {
    public void loginAs(String username, String password) {
        page.fill("input[autocomplete='username']", username);
        page.fill("input[autocomplete='current-password']", password);
        page.click("button[type='submit']");
    }
    
    public void assertErrorMessage(String expectedMsg) {
        assertThat(page.textContent(".alert-danger")).contains(expectedMsg);
    }
}

// LedgerPage: 專注於記帳工作台之互動與非同步斷言
public class LedgerPage extends BasePage {
    public void submitQuickInput(String text) {
        page.fill("input[placeholder*='例如']", text);
        page.click("button:has-text('解析並記帳')");
    }

    public void assertSweetAlertSuccess(String title) {
        // Playwright 自動等待 SweetAlert2 彈出視窗出現
        page.waitForSelector(".swal2-popup.swal2-icon-success");
        assertThat(page.textContent(".swal2-title")).contains(title);
    }

    public String getTotalExpenseText() {
        return page.textContent("#total-expense-amount");
    }
}
```

---

## 6. 核心業務黃金路徑測試矩陣 (Golden Path Matrix)

```
+----------------------------------------------------------------------------------------------------+
| 編號 | 測試案例名稱                          | 層級   | 驗證要點與預期結果                                     |
+----------------------------------------------------------------------------------------------------+
| 01   | AuthApi_RegisterAndLogin_Success      | API    | 註冊成功 -> 密碼 Hash -> 登入取得有效 JWT Bearer Token |
| 02   | AuthApi_DuplicateUsername_Conflict    | API    | 重複註冊同名帳號 -> 預期 409 Conflict                  |
| 03   | AuthApi_InvalidPassword_Unauthorized  | API    | 錯誤密碼登入 -> 預期 401 Unauthorized                  |
| 04   | LedgerApi_FullLifecycle_Success       | API    | 建立分類 -> 記帳 -> 查月報表 (金額相符) -> 刪除記帳   |
| 05   | Security_TenantIsolation_Forbidden    | API    | 用戶 B 嘗試存取/刪除用戶 A 之記帳與分類 -> 預期 403/404 |
| 06   | UI_UserLoginAndDashboardRedirect      | UI真機 | 輸入正確帳號密碼 -> 導向 /ledger -> LocalStorage 有 Token|
| 07   | UI_QuickSmartAccountingFlow           | UI真機 | 輸入 "午餐 120" -> Swal2 彈窗 -> 表格出現 -> 餘額即時變動|
| 08   | UI_UserLogoutFlow                     | UI真機 | 點擊登出 -> LocalStorage Token 被清除 -> 重導向 /login  |
+----------------------------------------------------------------------------------------------------+
```

---

## 7. Maven 構建分流策略 (Test Splitting)

為了確保工程師在平日開發與存檔時不會被十幾秒的瀏覽器啟動延遲中斷思緒，必須在 `pom.xml` 中進行測試分流：

```
+----------------------------------------------------------------------------------------------------+
|                                    Maven 測試分流執行架構                                          |
+----------------------------------------------------------------------------------------------------+

   [平日開發循環]
   執行 ./mvnw.cmd test (Maven Surefire Plugin)
         |
         +--> 僅執行純單元測試 (*Test.java)
         +--> 毫秒級全綠燈通過，極速反饋

   [整合驗收 / CI / PR 審查]
   執行 ./mvnw.cmd verify (Maven Failsafe Plugin)
         |
         +--> 步驟 1: 執行單元測試 (*Test.java)
         +--> 步驟 2: 打包並啟動 Spring Boot 隨機埠
         +--> 步驟 3: 執行全鏈路與真機測試 (*IT.java / *E2ETest.java)
         +--> 步驟 4: 自動關閉伺服器並產出驗收報告
```

---

## 8. 後續推進計畫與銜接

本探索報告已完整釐清架構藍圖與選型決策，後續可透過正式 OpenSpec 變更提案進行工程化落地：

1. **提案建立**：執行 `openspec new change "add-e2e-testing-framework"`。
2. **依賴引入**：在 `pom.xml` 中配置 `playwright` (1.46+) 與 `maven-failsafe-plugin`。
3. **基礎架構實作**：建立 `application-test.yml`、`PlaywrightTestBase` 與 `TestUserFactory`。
4. **POM 與案例落地**：依序完成 API 整合測試與 Playwright UI 真機測試。
5. **納入品質檢核**：將 `mvn verify` 納入 [專案通用工程標準 (engineering_standards_and_code_cleanliness.md)](../specifications/engineering_standards_and_code_cleanliness.md) 的交付檢核清單（DoD）。
