# 每日記帳系統 (Daily Ledger System) - 整合測試範疇、實施策略與 SQL Server 介接評估報告

> **文件版本**：v3.0.0 (tibame_account_test 實裝驗收、集中式 Fixture 與端點整合定案版)  
> **更新日期**：2026-09-04  
> **技術棧**：Java 21 / Spring Boot 3.3.13 / JUnit 5 / MockMvc / TestRestTemplate / Playwright / H2 In-Memory DB (MODE=MSSQLServer) / Microsoft SQL Server 2022  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md) ｜ [v1.0 歷史版本存檔](integration_testing_scope_and_strategy_exploration_v1.0.md) ｜ [v2.0 歷史版本存檔](integration_testing_scope_and_strategy_exploration_v2.0.md) ｜ [本地資料庫憑證注入手冊](../specifications/local_database_credentials_and_ide_injection_guide.md) ｜ [自動化測試架構篇](automated_testing_strategy_and_exploration.md) ｜ [單元測試手冊](../specifications/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md) ｜ [E2E 測試手冊](../specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md)  

---

## 1. 探索背景與現階段成果驗收 (Executive Summary & Current Progress)

本專案「每日記帳系統（Daily Ledger System）」在軟體工程品質上已建立起兩道堅實的自動化防線：
1. **底層單元測試（Surefire 66 個案例）**：涵蓋密碼學演算法（AES-256-GCM / BCrypt）、密碼政策校驗、JWT 強型別配置治理、YAML 鍵名轉義防禦、自然語言正則解析與業務服務純計算邏輯，在 6 秒內極速反饋。
2. **頂層 E2E 測試（Failsafe 10 個核心情境）**：涵蓋以 `TestRestTemplate` 執行的 API 整合驗證（Auth / Ledger / TenantIsolation）與以 Playwright 驅動之 Chromium 真機 UI 流程驗收。

### 1.1 現階段基礎建設推進成果 (Level 1 已就緒)
截至目前，專案已順利完成針對 Microsoft SQL Server 真機整合測試的底層基礎設施搭建：
* **獨立測試資料庫配置**：完成 `src/test/resources/application-test-mssql.yml`，嚴格對接獨立測試庫 `tibame_account_test`，支援環境變數與預設本機連線參數注入。
* **共用測試抽象基底**：建立 `src/test/java/com/tibame/integration/base/IntegrationTestBase.java`，統一定義 `@SpringBootTest(webEnvironment = MOCK)`、`@AutoConfigureMockMvc` 與類別層級 `@Transactional` 自動回滾。
* **首座整合測試驗證**：建立 `src/test/java/com/tibame/integration/DatabaseIntegrationIT.java`，實證驗證了 11 筆系統種子資料自動初始化、動態主鍵指派（防範 IDENTITY 跳號）以及交易回滾機制。

### 1.2 當前核心任務：推進業務領域整合測試 (Level 2 & Level 3)
在基礎環境打通後，核心目標為建立**高覆蓋率、極速反饋（2~3 秒內）、且能同時在 H2 與真實 SQL Server 上無縫切換的整合測試矩陣**，全面補足單元測試無法觸及的 7 大核心持久層與安全盲區。

```
+-----------------------------------------------------------------------------------+
|                        專案整合測試推進進度與分層落點                             |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ Level 1：環境與連線基礎設施 ] ───> 【現況：已完成 [x]】                        |
|  - application-test-mssql.yml (tibame_account_test 專屬庫連線)                    |
|  - IntegrationTestBase.java (@SpringBootTest + @AutoConfigureMockMvc)             |
|  - DatabaseIntegrationIT.java (驗證種子資料載入與 IDENTITY 動態主鍵)              |
|                                                                                   |
|  [ Level 2：共用測試輔助工具 (Fixtures & Helpers) ] ───> 【v3.0 拍板重點】        |
|  - 擴充 IntegrationTestBase：Token 快速合成、JSON 序列化、測試用戶工廠            |
|                                                                                   |
|  [ Level 3：業務邊界與 7 大盲區整合測試矩陣 ] ────> 【v3.0 拍板重點】             |
|  - AuthIntegrationIT (安全防護、401 攔截、@Valid 參數校驗)                         |
|  - CategoryIntegrationIT (分類唯一性、403 系統保護、409 關聯刪除衝突)             |
|  - LedgerIntegrationIT (記帳 CRUD、NLP 解析落庫、COALESCE 月度統計聚合)           |
|  - LedgerSpecificationIT (動態多條件組合查詢、倒序分頁真實 SQL 方言 - 選項 A)     |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. 核心架構機制校正：安全防護鏈真實流向 (Security Architecture Reality)

在整合測試的斷言設計中，必須忠實反映專案的真實安全性架構。本專案採用自訂輕量安全過濾架構：

```
+----------------------------------------------------------------------------------------------------+
|                               安全防護鏈與例外轉譯真實鏈路全景                                     |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ HTTP 請求 (MockMvc) ]                                                                           |
|       |                                                                                            |
|       v                                                                                            |
|  [ 1. JwtAuthenticationFilter ] (OncePerRequestFilter)                                             |
|       | - 解析 Header: Authorization: Bearer <token>                                               |
|       | - 若合法: 解析 userId/username 寫入 UserContext (ThreadLocal)                               |
|       | - 若無 Token 或 Token 畸形/過期: 不中斷請求，直接放行 (filterChain.doFilter)               |
|       v                                                                                            |
|  [ 2. 進入 ApiController 端點 ]                                                                    |
|       | - 先觸發 Spring MVC @Valid 參數校驗 (若不符規則直接拋出 MethodArgumentNotValidException)   |
|       | - 業務開頭顯式呼叫: Long userId = UserContext.requireUserId();                             |
|       | - 若 UserContext 為 null ➔ 拋出 UnauthorizedException(401)                                 |
|       v                                                                                            |
|  [ 3. 業務服務層 ServiceImpl ]                                                                     |
|       | - 執行 @Transactional 業務邏輯、多租戶隔離查詢、跨實體約束校驗                             |
|       v                                                                                            |
|  [ 4. 全域例外處理 GlobalExceptionHandler ]                                                        |
|       | - 攔截 UnauthorizedException / ApiException ➔ 封裝為標準 ApiResponse(code, message)        |
|       | - 攔截 MethodArgumentNotValidException ➔ 封裝為標準 ApiResponse(400, "第一筆欄位錯誤訊息") |
|       v                                                                                            |
|  [ 5. Filter finally 區塊 ]                                                                        |
|       | - 強制執行 UserContext.clear() 銷毀 ThreadLocal，徹底防禦執行緒池身分洩漏                   |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

> **關鍵架構結論**：  
> 未授權請求（無 Token 或偽造 Token）**並非在 Filter 階段被阻斷，而是完整通過 Filter 進入 Controller 後，由 `UserContext.requireUserId()` 拋出 `UnauthorizedException`，最終由 `GlobalExceptionHandler` 統一轉譯為 HTTP 401 回應**。整合測試的斷言設計必須忠實檢驗此一完整流向。

---

## 3. 測試分工金字塔與整合測試 7 大核心盲區

```
+-----------------------------------------------------------------------------------+
|                           專案三層測試體系職責精準劃分                            |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   [ 頂層：E2E 端到端測試 (Playwright & TestRestTemplate) ]                         |
|   - 檔案：*E2ETest.java                                                           |
|   - 模式：黑箱真實 HTTP (RANDOM_PORT) / 真機 Chromium 瀏覽器                        |
|   - 任務：驗收「黃金業務旅程 (Happy Path)」與全站 UI 渲染                         |
|                                                                                   |
|   [ 中層：整合測試 (Spring Boot Test + MockMvc) ]  <--- 【本次擬議核心】          |
|   - 檔案：*IT.java                                                                |
|   - 模式：灰箱記憶體 DispatcherServlet，結合真實 Service / Repository / 資料庫     |
|   - 任務：專攻「邊界條件、異常攔截、跨實體約束、動態查詢與交易回滾」               |
|                                                                                   |
|   [ 底層：單元測試 (JUnit 5 + Mockito) ]                                          |
|   - 檔案：*Test.java (目前 66 案例)                                               |
|   - 模式：純白箱，無 Spring 容器，極速反饋 (Surefire ~6 秒)                       |
|   - 任務：演算法、密碼學、自然語言正則解析、配置與元數據治理                       |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### 整合測試（IT）應專門補足的 7 大核心檢驗維度

```
+-----------------------------------------------------------------------------------+
|                             整合測試 7 大核心檢驗盲區                             |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. JPA Specification 動態多條件組合查詢 (模糊搜尋/分頁/倒序)                     |
|  2. 跨實體引用刪除防護 (引用分類刪除拋 409，刪除記帳後允許刪除)                   |
|  3. Controller @Valid 參數校驗與例外轉譯格式 (密碼強度/負數金額)                  |
|  4. 安全防護邊界 (偽造 Token/過期 Token/無 Bearer 前綴回傳 401)                    |
|  5. 分類唯一性與租戶隔離 (同用戶同名 409 衝突 vs 不同用戶同名允許)                 |
|  6. 系統預設分類保護 (is_system=true 嘗試修改/刪除回傳 403)                        |
|  7. 月度統計聚合邊界 (無資料時 COALESCE 回傳 0.00，跨月精準過濾)                   |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 4. 實施途徑深化：Spring Context 快取與集中式 Fixture 架構

### 4.1 單一全上下文架構 (Single Context)
為了避免測試執行緩慢，嚴格禁止在整合測試中使用切片註解（如 `@DataJpaTest` 或 `@WebMvcTest`），所有 `*IT.java` 必須統一繼承 `IntegrationTestBase`。
* **效益**：整個測試套件共用唯一的 `ApplicationContext`，只冷啟動一次（約 3~4 秒），後續所有測試類別共用快取，每個測試方法僅需數十毫秒。
* **資料庫無污染保證**：依賴類別層級的 `@Transactional`，測試執行後自動發出 `ROLLBACK`，完全不需要使用破壞快取的 `@DirtiesContext`。

### 4.2 身分準備機制深度對比：每個類別自己註冊 vs 集中在 Base 提供

在測試中準備已授權用戶身分時，存在兩種途徑。本專案拍板定案採用**途徑二（集中在 Base 提供）**：

```
+----------------------------------------------------------------------------------------------------+
|                                    兩種身分準備方式架構對比                                         |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ 途徑一：每個測試類別自己發起 HTTP 註冊/登入 (不推薦) ]                                          |
|    測試案例 A ──> POST /api/v1/auth/register (BCrypt 加密 ~80ms) ──> POST /login (BCrypt ~80ms)    |
|    測試案例 B ──> POST /api/v1/auth/register (BCrypt 加密 ~80ms) ──> POST /login (BCrypt ~80ms)    |
|    - 缺點：40 個測試案例將重複執行 80 次 BCrypt 密碼學計算，耗時增加 6~8 秒！                     |
|    - 缺點：若 Auth 註冊 DTO 修改，所有無關的 CategoryIT、LedgerIT 全部跟著報錯 (假失敗)           |
|                                                                                                    |
|  [ 途徑二：集中在 IntegrationTestBase 提供 Fixture Helper (專案拍板採用) ]                         |
|    測試案例 A ──┐                                                                                  |
|    測試案例 B ──┼─> createTestUser("alice")                                                        |
|    測試案例 C ──┘     ├─> userRepository.save(已準備好 BCrypt 假密碼)  <1ms                        |
|                       └─> tokenService.generateToken(userId, username) <1ms                        |
|    - 優點：完全跳過昂貴的 CPU 密碼運算，每個案例準備身分僅需 1 毫秒！                              |
|    - 優點：測試方法結束後，@Transactional 自動 ROLLBACK，乾淨無殘留                                 |
|    - 優點：關注點分離，Category 與 Ledger 測試僅關注自己的核心邏輯                                |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

| 評估維度 | 途徑一：每個測試類別自己註冊 (走 HTTP) | 途徑二：集中在 Base 提供 (走記憶體 Helper) | 決策結果 |
| :--- | :--- | :--- | :--- |
| **執行效能** | **極慢**。重複 80 次 BCrypt 慢速雜湊，需耗費 **6~8 秒**。 | **極快 (< 1ms)**。寫入 Entity 並在記憶體簽發 JWT，整套僅需 **1~2 秒**。 | **途徑二勝出** |
| **關注點分離** | **緊密偶合**。`CategoryIT` 的成敗竟取決於註冊 API 格式。 | **完全解偶**。註冊 API 由 `AuthIntegrationIT` 專責覆蓋。 | **途徑二勝出** |
| **程式碼簡潔度** | **冗餘繁瑣**。每個 IT 類別都要宣告 DTO 與 JSON 解析。 | **極致乾淨**。只需一行：`var user = createTestUser("bob");`。 | **途徑二勝出** |
| **多租戶模擬** | **繁複**。需連發 4 次 HTTP 請求來建立兩個帳號。 | **極為優雅**。兩行即可建立兩個相互隔離的租戶 Context。 | **途徑二勝出** |

### 4.3 `IntegrationTestBase` 擴充藍圖

```java
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TokenService tokenService;

    // 事先計算好符合政策的 BCrypt 密碼雜湊 ("TestPass123!#")，跳過昂貴運算
    private static final String PRE_ENCRYPTED_PASSWORD_HASH = 
            "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW";

    public record TestUserContext(User user, String token) {
        public String bearerToken() {
            return "Bearer " + token;
        }
    }

    /**
     * 快速建立測試用戶並簽發有效 JWT Token (耗時 < 1ms)
     */
    protected TestUserContext createTestUser(String prefix) {
        String unique = prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
        User user = User.builder()
                .username(unique)
                .passwordHash(PRE_ENCRYPTED_PASSWORD_HASH)
                .email(unique + "@example.com")
                .displayName("Test User " + unique)
                .build();
        User savedUser = userRepository.save(user);
        String token = tokenService.generateToken(savedUser.getId(), savedUser.getUsername());
        return new TestUserContext(savedUser, token);
    }

    protected String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}
```

---

## 5. 整合測試套件模組劃分與實施矩陣 (The 4 IT Suites Matrix)

將 7 大核心盲區收斂並對齊專案現有 3 個 API 控制器與 1 個動態查詢 Specification，劃分出 4 個領域類別：

### 5.1 `AuthIntegrationIT.java`（認證、授權與安全性邊界）
* **待測端點**：`/api/v1/auth/register`、`/api/v1/auth/login`、`/api/v1/auth/me`
* **覆蓋盲區**：**盲區 3** (@Valid 校驗) 與 **盲區 4** (安全防護邊界)
* **核心案例矩陣**：
  1. `testRegisterPasswordPolicyViolation_Returns400()`：密碼未滿 6 碼或無特殊符號，斷言 HTTP 400 且回應包含規格錯誤訊息。
  2. `testAccessMeWithoutToken_Returns401()`：無 `Authorization` Header 存取 `/me`，斷言 401。
  3. `testAccessMeWithTamperedToken_Returns401()`：竄改簽名之偽造 Token 存取，斷言 401。
  4. `testAccessMeWithInvalidBearerFormat_Returns401()`：Header 缺少 `Bearer ` 前綴，斷言 401。
  5. `testLoginSuccessAndFailureFlow()`：正確帳密登入回傳 200 與有效 Token，錯誤密碼回傳 401。

### 5.2 `CategoryIntegrationIT.java`（分類管理、租戶隔離與業務約束防護）
* **待測端點**：`POST/PUT/DELETE /api/v1/categories`
* **覆蓋盲區**：**盲區 2** (引用刪除防護)、**盲區 5** (分類唯一性) 與 **盲區 6** (系統分類保護)
* **核心案例矩陣**：
  1. `testDeleteCategoryWithExistingRecords_Returns409()`：
     - 建立分類並新增一筆關聯記帳。
     - 嘗試刪除分類 ➔ 斷言觸發 `countByCategoryId > 0` 拋出 **409 Conflict**。
     - 刪除關聯記帳後再次刪除分類 ➔ 斷言刪除成功 (**200 OK**)。
  2. `testCategoryUniquenessAndTenantIsolation()`：
     - 用戶 A 建立同名分類兩次 ➔ 斷言第二次拋出 **409 Conflict**。
     - 用戶 B 建立同名分類 ➔ 斷言成功建立 (**201 Created**)，證明租戶隔離性。
  3. `testModifyOrDeleteSystemCategory_Returns403()`：
     - 對 `is_system = true` 之種子分類（如 ID 1）發起 `PUT` 或 `DELETE` ➔ 斷言回傳 **403 Forbidden**。

### 5.3 `LedgerIntegrationIT.java`（記帳業務、NLP 快記與統計聚合）
* **待測端點**：`/api/v1/records`、`/api/v1/records/quick`、`/api/v1/records/summary`
* **覆蓋盲區**：**盲區 7** (月度統計聚合邊界) 與業務多租戶隔離
* **核心案例矩陣**：
  1. `testRecordTenantIsolation_CrossUserAccessForbidden()`：
     - 用戶 A 建立記帳，用戶 B 嘗試讀取、修改或刪除 ➔ 斷言 404 或 403。
  2. `testQuickCreateRecord_NlpParsedAndPersisted()`：
     - 傳入 `{"text": "午餐吃牛肉麵 180"}` ➔ 斷言正確提取金額 `180`、描述並自動匹配「餐飲食品」分類落庫。
  3. `testMonthlySummary_EmptyDataReturnsZero()`：
     - 當月無任何記帳資料時查詢統計 ➔ 驗證 `COALESCE(SUM, 0)` 正確回傳 `0.00`，不引發 NPE。
  4. `testMonthlySummary_CrossMonthBoundaryFiltering()`：
     - 寫入 1/31 與 2/1 資料，查詢 1 月統計 ➔ 驗證跨月過濾精確無誤。

### 5.4 `LedgerSpecificationIT.java`（JPA Specification 動態查詢與真實 SQL 方言檢驗）
* **模式定案**：**選項 A（MockMvc 端點整合模式：`GET /api/v1/records?...`）**
* **覆蓋盲區**：**盲區 1** (動態多條件組合查詢與倒序分頁)
* **核心案例矩陣**：
  1. `testDynamicMultiCriteriaSpecification()`：
     - 建立多筆不同日期、收支類型、分類與備註的紀錄。
     - 同時傳入 `startDate`, `endDate`, `recordType`, `categoryId`, `keyword` 參數。
     - 驗證真實 SQL 產生正確的 `AND` 條件與 `LIKE %keyword%` 模糊匹配。
  2. `testPaginationAndDefaultSorting()`：
     - 驗證分頁大小（`size=15`）、頁碼切換，以及預設依據 `recordDate DESC, id DESC` 倒序排序之結果。
     - 在 SQL Server 實例下真實檢驗 Hibernate `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` 方言之相容性。

---

## 6. 介接 Microsoft SQL Server 資料庫之測試策略與防坑實踐

### 6.1 H2 模擬的 4 大盲區
1. **識別列跳號（IDENTITY Column & Rollback）**：SQL Server 在交易回滾後，`IDENTITY(1,1)` 序號**不會退回**。
2. **定序與中文編碼（Collation）**：SQL Server 的 `Chinese_Taiwan_Stroke_CI_AS` 大小寫不敏感與 `NVARCHAR` 排序規則，H2 無法完全重現。
3. **併發鎖定與隔離層級（Locking & Isolation）**：SQL Server 預設為鎖定型 `READ COMMITTED`，與 H2 單純記憶體 MVCC 表現不同。
4. **方言與驅動程式特性**：Hibernate 對 `SQLServerDialect` 產生的 `OFFSET FETCH` 分頁與日期計算函數。

### 6.2 四大核心防坑實踐
1. **資料庫嚴格隔離**：連線庫固定為 `tibame_account_test`，絕不污染日常開發庫 `tibame_account`。
2. **主鍵絕不硬編碼**：因應 `IDENTITY` 跳號特性，**斷言一律為 `assertThat(id).isNotNull().isPositive()`**，並傳遞動態 ID。
3. **`@Transactional` 回滾保證**：測試結束自動發出 `ROLLBACK`，保證資料庫無髒資料殘留。
4. **開機 DDL 冪等性**：依賴 `schema.sql` 中的 `IF NOT EXISTS`，支援重複重用。

---

## 7. Maven 生命週期與 CI 管線調度配置

專案已透過 `pom.xml` 完成測試生命週期分流：
* **`maven-surefire-plugin`**：專門執行單元測試，排除 `**/*IT.java` 與 `**/*E2ETest.java`。
* **`maven-failsafe-plugin`**：專門執行整合與端到端測試，包含 `**/*IT.java` 與 `**/*E2ETest.java`。

### 指令調度速查矩陣

| 驗證情境 | 執行指令 | 涵蓋範疇 | 預期耗時 | 適用場景 |
| :--- | :--- | :--- | :--- | :--- |
| **快速單元驗證** | `./mvnw test` | 66 個純單元測試 (Surefire) | 約 5~6 秒 | 本機日常存檔、PR 快速門禁 (`ci-pr.yml`) |
| **隨選整合測試 (H2)** | `./mvnw test-compile failsafe:integration-test -Dit.test="*IT"` | 僅執行 `*IT.java` (MockMvc + H2) | 約 3~5 秒 | 修改 Controller、Service 交易或查詢時 |
| **真實 MSSQL 測試** | `./mvnw test-compile failsafe:integration-test -Dspring.profiles.active=test-mssql -Dit.test="*IT"` | 僅執行 `*IT.java` 直連 `tibame_account_test` | 約 5~7 秒 | 驗證 SQL Server 方言、約束與交易時 |
| **全量深度驗收** | `./mvnw verify` | Surefire + Failsafe (`*IT` + Playwright `*E2ETest`) | 約 20~30 秒 | 主分支合併深度門禁 (`ci-main.yml`) |

---

## 8. 後續推進建議與 OpenSpec 落地路徑 (Actionable Next Steps)

1. **擴充測試基底 `IntegrationTestBase.java`**：
   - 注入 `ObjectMapper`、`UserRepository`、`TokenService`。
   - 提供 `createTestUser(prefix)` 預算 BCrypt 雜湊之高頻 Helper，將測試準備時間降至 1ms 內。
2. **依序落實 4 大整合測試套件**：
   - 第一優先：`AuthIntegrationIT`（覆蓋 401 攔截與密碼強度）
   - 第二優先：`CategoryIntegrationIT`（覆蓋 409 刪除防護與租戶唯一性）
   - 第三優先：`LedgerIntegrationIT`（覆蓋 NLP 落庫與 COALESCE 月度統計）
   - 第四優先：`LedgerSpecificationIT`（選項 A，覆蓋 Specification 動態查詢與分頁倒序）
3. **發起 OpenSpec 變更提案**：
   - 透過 `/opsx-propose add-backend-integration-tests` 發起標準變更，收斂驗收條件（DoD）並執行實施。
