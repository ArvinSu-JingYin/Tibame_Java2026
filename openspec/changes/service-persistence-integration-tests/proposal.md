## Why

現有測試體系存在「頂層全端與底層純單元測試之間的重大中層斷層」：單元測試因高度 Mockito 虛擬化，無法驗證 Spring Data JPA 動態 Specification、自訂 JPQL 聚合語法以及資料庫外鍵級聯約束；而全端 API/UI 測試成本高且運行於獨立 HTTP 執行緒無法套用 `@Transactional` 自動回滾。導入服務層持久化整合測試可填補此空白，在無 Web 伺服器負擔下直接連動真實 JPA 與資料庫引擎，達成「真實持久化、極速反饋、交易自動回滾零髒資料」目標。

## What Changes

- **建立服務層持久化整合測試基底**：新增 `ServiceIntegrationTestBase.java`，宣告 `@SpringBootTest(webEnvironment = NONE)`、類別層級 `@Transactional` 與 `@ActiveProfiles("test")`，並提供動態隨機使用者生成工廠。
- **流水帳業務持久化與多維查詢整合測試**：新增 `LedgerServicePersistenceIT.java`（6 大核心案例），驗證月度收支 JPQL 聚合統計 (`COALESCE/SUM`)、空值防禦、多維度 Specification 動態查詢、CRUD 生命週期、跨租戶橫向越權防禦 (IDOR) 與不存在分類外鍵防護。
- **分類管理與關聯約束整合測試**：新增 `CategoryServicePersistenceIT.java`（5 大核心案例），驗證系統種子分類唯讀保護、多租戶自訂分類可見性隔離、關聯流水帳刪除防禦 (`countByCategoryId > 0`)、無關聯分類安全刪除與同名重複約束。
- **認證服務與使用者資料落盤整合測試**：新增 `AuthServicePersistenceIT.java`（3 大核心案例），驗證註冊成功 BCrypt 雜湊落盤、重複使用者帳號衝突防禦與重複 Email 唯一性防護。
- **雙軌資料庫驗證支援**：完全相容 H2 記憶體資料庫 (`test` profile) 與 MS SQL Server 2022 實體資料庫 (`test-mssql` profile)，支援極速本機驗證與真實資料庫方言驗收。

## Capabilities

### New Capabilities
- `service-persistence-integration-testing`: 定義服務層與 JPA 持久化整合測試架構，涵蓋交易自動回滾、JPQL 聚合計算、動態 Specification 查詢、多租戶隔離防禦與實體關聯完整性驗證規範。

### Modified Capabilities
<!-- 無既有需求規範變更 -->

## Impact

- **被測原始碼**：無破壞性變更。直接覆蓋並驗證 `LedgerService`、`CategoryService`、`AuthService`、`AccountRecordRepository`、`CategoryRepository` 與 `UserRepository`。
- **測試框架與結構**：於 `src/test/java/com/tibame/integration/` 目錄下新增 `base/ServiceIntegrationTestBase.java` 及各業務服務之 `*PersistenceIT.java`。
- **建置生命週期**：納入 Maven Failsafe 整合測試生命週期 (`mvn verify` / `mvn test -Dtest=*PersistenceIT`)，執行耗時維持在數百毫秒等級。
- **依賴與系統**：相容現有 Spring Boot 3.3.13、Spring Data JPA、H2 與 Microsoft SQL Server 2022，無新增外部相依套件。
