# 每日記帳系統 (Daily Ledger System) - 整合測試範疇、實施策略與 SQL Server 介接評估報告

> **文件版本**：v2.0.0 (架構校準與 SQL Server 介接擴充版)  
> **更新日期**：2026-09-04  
> **技術棧**：Java 21 / Spring Boot 3.3.13 / JUnit 5 / MockMvc / TestRestTemplate / Playwright / H2 In-Memory DB (MODE=MSSQLServer) / Microsoft SQL Server 2022  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md) ｜ [v1.0 歷史版本存檔](integration_testing_scope_and_strategy_exploration_v1.0.md) ｜ [本地資料庫憑證注入手冊](../specifications/local_database_credentials_and_ide_injection_guide.md) ｜ [自動化測試架構篇](automated_testing_strategy_and_exploration.md) ｜ [單元測試手冊](../specifications/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md) ｜ [E2E 測試手冊](../specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md)  

---

## 1. 探索背景與問題意識 (Executive Summary & Background)

本專案「每日記帳系統（Daily Ledger System）」在軟體工程品質上已建立起兩道堅實的自動化防線：
1. **底層單元測試（Surefire 66 個案例）**：涵蓋密碼學演算法（AES-256-GCM / BCrypt）、密碼政策校驗、JWT 強型別配置治理、YAML 鍵名轉義防禦、自然語言正則解析與業務服務純計算邏輯，在 6 秒內極速反饋。
2. **頂層 E2E 測試（Failsafe 10 個核心情境）**：涵蓋以 `TestRestTemplate` 執行的 API 整合驗證（Auth / Ledger / TenantIsolation）與以 Playwright 驅動之 Chromium 真機 UI 流程驗收。

然而，在純 Java 邏輯的單元測試與開銷較重的頂層 E2E 之間，存在一塊極為關鍵的**測試盲區（Testing Gap）**：
* **單元測試「測不到」真實持久層行為**：單元測試中所有 JPA Repository 皆為 Mockito 打樁，無法驗證真實 SQL 語法、H2/MSSQL 方言、`Specification` 動態條件組裝、JPQL 聚合計算（如 `COALESCE(SUM, 0)`）、資料庫唯一性約束（Unique Constraint）與外鍵引用的真實級聯行為。
* **單元測試「測不到」自訂安全過濾鏈與交易邊界**：單元測試直接調用 Service 方法傳入 `userId`，無法驗證未授權請求穿越過濾器進入 Controller 呼叫 `requireUserId()` 觸發 401 的完整鏈路，亦無法檢驗 `@Transactional(rollbackFor = Exception.class)` 在業務拋出異常時是否真實回滾資料庫。
* **頂層 E2E 測試「難以窮舉邊界且開銷過重」**：既有 E2E API 測試運行於真實隨機 Port，主要聚焦於「黃金業務旅程（Happy Path）」。若將所有 DTO `@Valid` 邊界校驗、畸形 Token 攔截、跨實體刪除衝突等負向場景皆由 E2E 覆蓋，將導致建置時間過長，且難以精準斷言 Controller 層級的例外細節。

因此，構建完善的**整合測試（Integration Testing）**體系，並確立**介接 Microsoft SQL Server** 的具體測試策略，是鞏固系統穩定度、確保多租戶隔離防禦與資料一致性的必要環節。

---

## 2. 核心架構機制校正：安全防護鏈真實流向 (Security Architecture Reality)

在整合測試的設計中，必須精確對齊專案的真實安全性架構。本專案採用自訂輕量安全過濾架構（無重量級 Spring Security Web Filter Chain 依賴）：

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
> 未授權請求（無 Token 或偽造 Token）**並非在 Filter 階段被阻斷，而是完整通過 Filter 進入 Controller 後，由 `UserContext.requireUserId()` 拋出 `UnauthorizedException`，最終由 `GlobalExceptionHandler` 統一轉譯為 HTTP 401 回應**。整合測試的斷言設計必須忠實反映此流向。

---

## 3. 測試分工金字塔與整合測試 7 大核心盲區

為了杜絕測試冗餘，系統三層測試體系的分工邊界劃分如下：

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

1. **JPA Specification 動態多條件組合查詢**：
   * 待測端點：`GET /api/v1/records`。
   * 檢驗點：`startDate`、`endDate`、`recordType`、`categoryId`、`keyword` 模糊搜尋多條件動態拼接。驗證 `PageRequest` 倒序排序（`recordDate DESC, id DESC`）之真實 SQL 語法與分頁結果正確性（單元測試因 Repository 被 Mock 而完全測不到此處）。
2. **跨實體引用刪除防護（關鍵業務約束）**：
   * 待測端點：`DELETE /api/v1/categories/{id}`。
   * 檢驗點：當分類已被 `AccountRecord` 引用時，嘗試刪除該分類 ➔ 驗證觸發 `accountRecordRepository.countByCategoryId(id) > 0`，拋出 409 Conflict 衝突阻擋；先刪除關聯記帳後再次刪除分類 ➔ 驗證成功刪除 (200 OK)。
3. **Controller `@Valid` 參數校驗與例外轉譯格式**：
   * 待測端點：`/api/v1/auth/register`、`/api/v1/records`。
   * 檢驗點：註冊密碼未達 6 碼或缺少特殊符號、金額小於等於 0 或小數點超過 2 位 ➔ 驗證由 `GlobalExceptionHandler` 封裝回傳標準 `ApiResponse(400, "...")` 格式。
4. **安全防護邊界（Token 異常攔截）**：
   * 待測端點：受保護之 `/api/v1/auth/me`、`/api/v1/categories` 等。
   * 檢驗點：無 Header、無 `Bearer ` 前綴、非法字元、過期 Token 或竄改簽名之 Token ➔ 驗證一律回傳 401 Unauthorized。
5. **分類唯一性與租戶隔離**：
   * 待測端點：`POST /api/v1/categories`。
   * 檢驗點：同一個使用者在同一收支類型下建立同名分類 ➔ 拋出 409 Conflict；不同使用者建立同名自訂分類 ➔ 允許成功建立 (201 Created)。
6. **系統預設分類防竄改**：
   * 待測端點：`PUT /api/v1/categories/{id}`、`DELETE /api/v1/categories/{id}`。
   * 檢驗點：對 `is_system = true` 的系統分類嘗試修改或刪除 ➔ 驗證拋出 403 Forbidden。
7. **月度統計聚合 (JPQL COALESCE & SUM) 邊界**：
   * 待測端點：`GET /api/v1/records/summary`。
   * 檢驗點：當月完全無任何記帳紀錄時，驗證 `COALESCE(SUM, 0)` 安全回傳 `0.00` 而非 null 或拋出異常；驗證跨月（如 1/31 與 2/1）資料過濾的精確度。

---

## 4. 實施途徑評估重審：Spring Context 快取開銷與單一 Context 架構

在規劃整合測試架構時，必須深入考量 Spring TestContext Framework 的**上下文快取（Context Caching）機制**：

```
+-----------------------------------------------------------------------------------+
|                     Spring Context 快取效能對比 (Context Caching)                 |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ 途徑 A：混合切片架構 (已否決) ]                                                |
|    AuthIT (@SpringBootTest) ──────┐ (共用 Context #1, ~4s)                        |
|    CategoryIT (@SpringBootTest) ──┘                                               |
|    RecordRepoIT (@DataJpaTest) ───> [ 強制新建 Context #2, 額外 +3s ] (破壞快取)  |
|                                                                                   |
|  [ 途徑 B：單一全上下文架構 (專案最佳推薦) ]                                      |
|    AuthIT ────────┐                                                               |
|    CategoryIT ────┼──> 共用唯一 ApplicationContext (僅啟動一次，~4s)              |
|    LedgerIT ──────┤    後續所有測試類別共用快取，每個測試方法僅需數十毫秒！       |
|    RecordRepoIT ──┘                                                               |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### 為什麼放棄獨立的 `@DataJpaTest` 切片？
1. **快取破壞懲罰**：當測試套件中同時存在 `@SpringBootTest` 與 `@DataJpaTest` 時，Spring 判定兩者配置切片不同，**被迫啟動兩套完全獨立的 Spring 容器**。這會額外增加 3~5 秒冷啟動開銷，並佔用兩倍記憶體。
2. **全上下文已涵蓋持久層**：在 `@SpringBootTest(webEnvironment = MOCK)` 容器中，所有 JPA Repository、實體映射與 H2 資料庫本就已經完整就緒。我們**完全可以直接在全上下文中注入 `AccountRecordRepository`** 測試 Specification 動態查詢，既享有一致的環境，又能 100% 複用同一個 Context 快取。

### `@Transactional` 自動回滾與資料庫無污染保證
* 在 `MockMvc`（WebEnvironment.MOCK）架構下，測試執行緒與請求處於同一個 Thread。
* 測試類別標註 `@Transactional` 時，每個測試方法結束後 Spring 會**自動執行 ROLLBACK**。
* 這意味著測試新增的使用者、分類與記帳資料在測試結束時立即抹除，資料庫永遠維持開機時 `DataInitializer` 植入的 11 筆系統分類純淨狀態，**完全不需使用破壞快取的 `@DirtiesContext`**。

---

## 5. 介接 Microsoft SQL Server 資料庫之測試策略與防坑實踐

專案的核心目標資料庫為 **Microsoft SQL Server**。雖然日常測試使用 H2 `MODE=MSSQLServer` 極快，但面對真實生產環境，仍需考慮 SQL Server 的特殊機制。

### 5.1 H2 模擬的 4 大盲區
1. **識別列跳號（IDENTITY Column & Rollback）**：SQL Server 在交易回滾後，`IDENTITY(1,1)` 序號**不會退回**。若測試斷言寫死 `id == 1`，只要前續有交易回滾，後續測試必將失敗。
2. **定序與中文編碼（Collation）**：SQL Server 的 `Chinese_Taiwan_Stroke_CI_AS` 之大小寫不敏感、全形/半形處理與 `NVARCHAR` 排序規則，H2 無法完全重現。
3. **併發鎖定與隔離層級（Locking & Isolation）**：SQL Server 預設為鎖定型 `READ COMMITTED`（或需手動開啟 RCSI），而 H2 是單純的記憶體 MVCC，無法測出 SQL Server 上的 Deadlock 或鎖等待超時。
4. **方言與驅動程式特性**：Hibernate 對 `SQLServerDialect` 產生的 `OFFSET FETCH` 分頁、日期計算函數與 JDBC 參數 Unicode 轉譯（`sendStringParametersAsUnicode=true`）。

---

### 5.2 介接 SQL Server 的 3 大實施途徑對比

```
+----------------------------------------------------------------------------------------------------+
|                                    SQL Server 測試三大途徑全景                                     |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ 途徑 A：Testcontainers ]          [ 途徑 B：專屬 Test Profile ]       [ 途徑 C：CI Service 容器 ] |
|  - 測試執行時動態拉起 Docker 容器    - 對接本機/既有運行的 SQL Server    - GitHub Actions CI 原生外掛|
|  - Spring Boot 3.1+ @ServiceConn     - 配合環境變數與 launch.json 注入   - 主分支合併時背景運行      |
|                                                                                                    |
|  [優點]                              [優點]                              [優點]                      |
|  + 拋棄式環境，100% 乾淨無污染       + 啟動 0 秒延遲 (背景已在跑)        + 開發者電腦不需 Docker     |
|  + 本機不需預先安裝 SQL Server       + 最貼近本機日常開發手感            + CI 流程自動化，全團隊一致 |
|                                                                                                    |
|  [缺點]                              [缺點]                              [缺點]                      |
|  - 需 Docker 環境                    - 需手動維護測試庫與帳密            - 僅限 CI 環境，本機無法    |
|  - 首次拉取映像檔耗時 (~1.5GB)       - 開發者環境不一致可能導致假失敗      隨選即跑                  |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

#### 途徑 A：Testcontainers 容器化隨選即用（雲原生標準）
* **機制**：引入 `org.testcontainers:mssqlserver` 與 `spring-boot-testcontainers`。測試啟動時自動透過 Docker 啟動 `mcr.microsoft.com/mssql/server:2022-latest`，並透過 Spring Boot 3.1+ 的 `@ServiceConnection` 動態綁定連線資訊。
* **適用**：具備 Docker 環境，追求 Zero-Configuration「Clone 即測」的現代開發流程。

#### 途徑 B：專屬 Test Profile（對接本地既有 SQL Server，零 Docker 依賴）
* **機制**：建立 `src/test/resources/application-test-mssql.yml`，對接本地既有 SQL Server 實例上的獨立資料庫 `tibame_account_test`。透過 `./mvnw verify -Dspring.profiles.active=test-mssql -Dit.test="*IT"` 隨選執行。
* **適用**：本機已安裝 SQL Server 且追求 0 秒啟動延遲的日常深度排查。

#### 途徑 C：GitHub Actions Service Container（CI 深度門禁）
* **機制**：在 `.github/workflows/ci-main.yml` 中定義 `services.mssql` 官方容器。主分支合併時由 GitHub Actions 自動啟動容器並注入環境變數執行完整驗證。
* **適用**：CI 自動化全真環境驗收，無需開發者本機維持 Docker 或 SQL Server。

---

### 5.3 介接 SQL Server 測試的四大核心防坑實踐

```
+-----------------------------------------------------------------------------------+
|                        SQL Server 整合測試防禦最佳實踐                            |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. [ 資料庫嚴格隔離 ] ───> 專用 tibame_account_test 庫，絕不可動用開發或正式庫   |
|                                                                                   |
|  2. [ 主鍵絕不硬編碼 ] ───> SQL Server IDENTITY 跳號特性，Assert 僅檢驗 isNotNull |
|                                                                                   |
|  3. [ 交易自動回滾 ]   ───> 測試方法標註 @Transactional，測完資料庫自動 Rollback  |
|                                                                                   |
|  4. [ 開機 DDL 冪等性 ] ───> schema.sql 使用 IF NOT EXISTS，支援重用與平滑升級     |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

1. **資料庫嚴格隔離（Dedicated Test Database）**：
   * 測試連線資料庫名稱必須為 `tibame_account_test`，絕對禁止指向日常開發庫 `tibame_account`，防止測試過程的自動建表或資料清理抹除開發資料。
2. **主鍵動態斷言（No Hardcoded IDs）**：
   * 由於 SQL Server `IDENTITY` 在交易回滾後不退回計數器，**測試斷言嚴禁硬編碼 ID 值（如 `assertEquals(1L, id)`）**，一律斷言 `assertThat(id).isNotNull()` 並以動態取得的 ID 進行後續關聯驗證。
3. **MockMvc + `@Transactional` 回滾機制**：
   * 測試方法標註 `@Transactional`，在 SQL Server 上測試結束後發出 `ROLLBACK TRANSACTION`，保證既測到真實 SQL，又不在資料庫殘留髒資料。
4. **開機 DDL 冪等性支援**：
   * 專案既有的 `schema.sql` 具備標準 T-SQL 的 `IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = '...')` 與 `IDENTITY(1,1)`，確保測試庫初次建立或重複重用皆平滑安全。

---

### 5.4 專案最佳落地架構：三階金字塔分流 (The Tiered Strategy)

```
+-----------------------------------------------------------------------------------+
|                           專案建議之測試分流決策樹                                |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ Level 1：日常秒級回饋 (Local Dev & PR Gate) ]                                  |
|  - 模式：純單元測試 + H2 (MODE=MSSQLServer) MockMvc IT                            |
|  - 耗時：~6 秒 (mvn test)                                                         |
|  - 目的：極速驗證 Java 業務運算、DTO 校驗、過濾器與常規邏輯                       |
|                                                                                   |
|  [ Level 2：本機深度除錯 (Local On-Demand Debug) ]                                |
|  - 模式：切換 application-test-mssql.yml，直連本機已運行的 SQL Server             |
|  - 耗時：~8 秒                                                                    |
|  - 目的：排查疑難 SQL 語法、Specification 動態組裝、本地環境真實聯調              |
|                                                                                   |
|  [ Level 3：主分支深度驗收 (Main Branch CI / Nightly) ]                           |
|  - 模式：GitHub Actions Service Container (SQL Server 2022 官方映像)              |
|  - 耗時：~30 秒 (含容器健康檢查)                                                  |
|  - 目的：合併至 main 前的最後一道防線，100% 確保 SQL Server 生產環境相容性        |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 6. Maven 生命週期與 CI 管線調度配置

專案的 `pom.xml` 已具備清晰的測試外掛分流：
* **`maven-surefire-plugin`**：專門執行單元測試，配置排除 `**/*IT.java` 與 `**/*E2ETest.java`。
* **`maven-failsafe-plugin`**：專門執行整合與端到端驗收，配置包含 `**/*IT.java` 與 `**/*E2ETest.java`。

### 指令調度速查矩陣

| 驗證情境 | 執行指令 | 涵蓋範疇 | 預期耗時 | 適用場景 |
| :--- | :--- | :--- | :--- | :--- |
| **快速單元驗證** | `./mvnw test` | 66 個純單元測試 (Surefire) | 約 5~6 秒 | 本機編碼反饋、PR 快速門禁 (`ci-pr.yml`) |
| **隨選整合測試** | `./mvnw test-compile failsafe:integration-test -Dit.test="*IT"` | 僅執行 `*IT.java` (MockMvc + H2) | 約 4~6 秒 | 修改 API 控制器、Service 交易或 Specification 時 |
| **真實 MSSQL 測試**| `./mvnw test-compile failsafe:integration-test -Dspring.profiles.active=test-mssql -Dit.test="*IT"` | 僅執行 `*IT.java` 直連本機 SQL Server | 約 6~8 秒 | 驗證 SQL Server 專有方言與約束時 |
| **全量深度驗收** | `./mvnw verify` | Surefire + Failsafe (`*IT` + Playwright `*E2ETest`) | 約 20~30 秒 | 主分支合併深度驗收 (`ci-main.yml`) |

---

## 7. 後續推進建議 (Next Steps & Roadmap)

1. **建立整合測試基礎架構**：
   * 建立 `src/test/java/com/tibame/integration/base/IntegrationTestBase.java`，統一定義 `@SpringBootTest(webEnvironment = MOCK)`、`@AutoConfigureMockMvc`、`@ActiveProfiles("test")` 與 `@Transactional`，作為所有 `*IT.java` 的單一 Context 基礎。
   * 封裝 `obtainBearerToken(String username, String password)` 輔助方法，簡化測試中的身分模擬。
2. **分模組落地 7 大盲區測試案例**：
   * `AuthIntegrationIT.java`：覆蓋註冊、登入、401 攔截與密碼強度 `@Valid` 校驗。
   * `CategoryIntegrationIT.java`：覆蓋分類唯一性 409、系統分類 403 保護與已有記帳引用刪除防護。
   * `LedgerIntegrationIT.java`：覆蓋記帳新增、自然語言解析落庫、JPQL 月度統計聚合邊界。
   * `LedgerSpecificationIT.java`：專門深度檢驗 `AccountRecordRepository` 的多條件動態查詢與倒序分頁。
3. **收斂至 OpenSpec 變更提案**：
   * 正式實作時，可透過 `/opsx-propose` 發起 `add-backend-integration-tests` 變更提案，明確定義驗收產物與 DoD 檢核。
