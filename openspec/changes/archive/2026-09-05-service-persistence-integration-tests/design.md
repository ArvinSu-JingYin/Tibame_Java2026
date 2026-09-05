## Context

參閱 `proposal.md` 與 `docs/explorations/service_persistence_integration_testing_exploration.md`。

專案現有整合測試基底 `IntegrationTestBase.java` 採用 `webEnvironment = RANDOM_PORT`，專門服務於全端 HTTP API E2E 測試。然而，該架構由於 HTTP 請求運行於獨立工作執行緒，無法受 `@Transactional` 管理，導致無法使用 Spring 測試交易自動回滾機制。
為了驗證 Service 層與 JPA Repository 在真實資料庫上的行為（包括 JPQL `COALESCE/SUM`、動態 JPA Specification、外鍵約束防禦），需要設計一套專屬的服務層持久化整合測試架構。

## Goals / Non-Goals

**Goals:**
- 建立專用抽象基底類別 `ServiceIntegrationTestBase.java`，配置 `@SpringBootTest(webEnvironment = NONE)` 與類別層級 `@Transactional`，達成極速啟動與 100% 交易自動回滾（零髒資料）。
- 實裝三大核心業務服務持久化整合測試：`LedgerServicePersistenceIT`（6 案例）、`CategoryServicePersistenceIT`（5 案例）、`AuthServicePersistenceIT`（3 案例），共計 14 個測試案例。
- 以真實 Repository 取代 Mockito 虛擬模擬，直接校驗 SQL 生成、自訂 JPQL 聚合語法與外鍵完整性約束。
- 提供測試資料輔助工廠方法（動態隨機帳號、BCrypt 雜湊密碼生成），杜絕測試間唯一索引衝突。
- 支援 H2 記憶體資料庫 (`test`) 與本機 MS SQL Server 2022 (`test-mssql`) 雙軌平滑切換。

**Non-Goals:**
- 不涉及 Web 容器（Tomcat）啟動、HTTP 請求路由或 JSON 序列化驗證（由 API E2E 覆蓋）。
- 不涉及 Spring Security Filter 鏈路與 JWT HTTP Header 攔截（由 API E2E 覆蓋）。
- 不涉及前端 Vue 3 元件與瀏覽器 DOM 渲染（由 Playwright UI E2E 覆蓋）。
- 不變更任何既有生產環境業務邏輯（Service / Repository / Controller）介面或資料庫 Schema。

## Decisions

### 1. 測試環境設定：`webEnvironment = NONE`
- **決策**：在 `ServiceIntegrationTestBase` 宣告 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)`。
- **理由**：關閉 Web 伺服器啟動開銷，測試僅初始化 Spring IoC 容器與 JPA/Hibernate 持久層，單一套件執行耗時控制在 100ms - 250ms，大幅提升反饋速度。
- **替代方案**：
  - `webEnvironment = RANDOM_PORT`：啟動耗時長達數秒，且多執行緒環境無法回滾交易。
  - `@DataJpaTest`：僅載入 JPA 實體與 Repository，缺乏完整 Service 與共用組件（如 `PasswordService`）依賴注入，無法進行 Service 層端到端業務校驗。

### 2. 資料隔離與清理策略：類別層級 `@Transactional` 自動回滾
- **決策**：在基底類別層級宣告 `@Transactional`。
- **理由**：Spring TestContext 框架在測試方法執行完畢後預設自動觸發交易 ROLLBACK，確保資料庫零髒資料遺留，徹底消除測試順序依賴與髒資料污染。
- **替代方案**：
  - 手動清理（`@AfterEach repository.deleteAll()`）：執行效率低、容易因外鍵約束刪除順序報錯，且若測試異常中斷將遺留未清理之髒資料。

### 3. 命名慣例與套件組織：`*PersistenceIT.java`
- **決策**：
  - 基底類別放置於 `com.tibame.integration.base.ServiceIntegrationTestBase`。
  - 測試案例放置於 `com.tibame.integration.service.*PersistenceIT`。
- **理由**：採用 `IT` 後綴，使 Maven Failsafe 插件自動納入整合測試階段（`mvn verify`），且與純單元測試（`*Test.java`）清晰區隔，並允許開發者透過 `-Dtest=*PersistenceIT` 進行專用快速驗證。
- **替代方案**：
  - 命名為 `*ServiceTest.java`：會被 Maven Surefire 視為單元測試執行，破壞快速建置（`mvn test`）的毫秒級承諾。

### 4. 測試資料工廠方法：動態隨機帳號與 BCrypt 雜湊落盤
- **決策**：在基底類別提供 `createAndPersistTestUser()`，帳號名稱加上動態 UUID 後綴，並注入 `PasswordService` 進行標準 BCrypt 雜湊落地。
- **理由**：防止重複執行測試或並行執行時觸發使用者名稱唯一約束衝突 (`409 Conflict`)，並驗證密碼加密落地標準。

## Risks / Trade-offs

- **[Hibernate 一級快取 (First-Level Cache) 遮蔽真實 SQL 錯誤]**  
  → *緩解措施*：在特定驗證實體狀態落盤與更新生命週期的測試方法中，適時呼叫 `entityManager.flush()` 與 `entityManager.clear()`，或重新透過 Repository 執行新查詢，強制 Hibernate 發出真實 SQL 語句。
- **[H2 MSSQLServer 模式與 MS SQL Server 實體方言語法差異]**  
  → *緩解措施*：所有自訂 JPQL 語法（例如 `COALESCE/SUM`）與 JPA Specification 一律遵循標準 ANSI/JPA 規範，避免使用資料庫專有保留字或特有函數，並提供 `test-mssql` 設定檔支援實體驗證。
- **[測試類別增加導致 Spring 上下文重複啟動]**  
  → *緩解措施*：所有服務持久化整合測試均繼承相同的 `ServiceIntegrationTestBase`，共享相同的 Spring 上下文快取（Context Caching），避免重複啟動容器。
