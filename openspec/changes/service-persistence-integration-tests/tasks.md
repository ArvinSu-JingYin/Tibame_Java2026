## 1. 基礎架構建設 (Base Infrastructure Setup)

- [x] 1.1 建立服務層持久化整合測試抽象基底類別 `src/test/java/com/tibame/integration/base/ServiceIntegrationTestBase.java`，宣告 `@SpringBootTest(webEnvironment = NONE)`、類別層級 `@Transactional` 與 `@ActiveProfiles("test")`，注入共用 Repository 與密碼服務，並實裝 `createAndPersistTestUser()` 動態隨機使用者生成工廠，執行 `mvn test-compile` 驗證編譯無誤

## 2. 流水帳業務持久化測試實裝 (Ledger Service Persistence Integration)

- [x] 2.1 實裝 `src/test/java/com/tibame/integration/service/LedgerServicePersistenceIT.java`，覆蓋 TC-LEDGER-IT-01 至 TC-LEDGER-IT-06（月度 JPQL `COALESCE/SUM` 聚合運算、無記錄月份零值防禦、多維度 Specification 動態條件查詢、CRUD 完整生命週期、跨租戶橫向越權存取防禦、不存在分類外鍵防禦），執行 `mvn test -Dtest=LedgerServicePersistenceIT` 驗證 6 個案例全數通過

## 3. 分類管理與關聯約束測試實裝 (Category Service Persistence Integration)

- [x] 3.1 實裝 `src/test/java/com/tibame/integration/service/CategoryServicePersistenceIT.java`，覆蓋 TC-CAT-IT-01 至 TC-CAT-IT-05（系統種子分類唯讀保護、多租戶自訂分類可見性隔離、關聯流水帳之自訂分類刪除防禦、無關聯分類安全刪除、同名重複衝突防禦），執行 `mvn test -Dtest=CategoryServicePersistenceIT` 驗證 5 個案例全數通過

## 4. 認證服務與使用者資料落盤測試實裝 (Auth Service Persistence Integration)

- [x] 4.1 實裝 `src/test/java/com/tibame/integration/service/AuthServicePersistenceIT.java`，覆蓋 TC-AUTH-IT-01 至 TC-AUTH-IT-03（註冊成功與 BCrypt 雜湊 `$2a$10$` 落盤驗證、重複使用者帳號衝突防禦、重複 Email 註冊衝突防禦），執行 `mvn test -Dtest=AuthServicePersistenceIT` 驗證 3 個案例全數通過

## 5. 雙軌資料庫驗收與生命週期校驗 (Dual Database Verification & Lifecycle Validation)

- [x] 5.1 執行全量服務層持久化整合測試極速驗收：執行 `mvn test -Dtest=*PersistenceIT`，驗證在 H2 記憶體資料庫環境下 14 個案例全數綠燈通過且無髒資料遺留
- [x] 5.2 執行實體資料庫方言驗收：執行 `mvn test -Dtest=*PersistenceIT -Dspring.profiles.active=test-mssql`，驗證在 Microsoft SQL Server 2022 實體資料庫環境下方言查詢、IDENTITY 跳號與事務回滾全數綠燈通過
