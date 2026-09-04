## Why

開發團隊已於本機 Microsoft SQL Server 實例上建立了專屬測試資料庫 `tibame_account_test`。為了兼顧日常 CI/PR 的秒級測試反饋（H2 記憶體模式），並提供直連真實微軟 SQL Server 的真機整合測試環境，必須建立標準化的測試配置、交易自動回滾基底與防禦準則，避免手動測試資料遭受破壞，並排查 SQL Server 特有語法、條件約束與跳號問題。

## What Changes

- **新增專屬測試設定檔**：新增 `src/test/resources/application-test-mssql.yml`，直連本機 `tibame_account_test` 資料庫，配置 `spring.sql.init.mode: always` 自動執行 `schema.sql` 與 `data.sql`，並支援密碼覆寫與回退機制。
- **建立整合測試共用基底**：建立 `src/test/java/com/tibame/integration/base/IntegrationTestBase.java`，配置 `@SpringBootTest`、`@AutoConfigureMockMvc` 與 `@Transactional`，確保測試案例執行完畢後交易自動 Rollback，保持測試資料庫潔淨。
- **納入 YAML 靜態語法檢查**：擴充 `YamlConfigurationLintTest` 掃描範圍，將 `application-test-mssql.yml` 納入 Map Key 括號轉義規範驗證。
- **建立 SQL Server 測試防禦標準**：規範真機整合測試案例嚴禁硬編碼實體主鍵 ID（防範 `IDENTITY(1,1)` 跳號機制），一律採用動態 ID 斷言（`assertThat(id).isNotNull().isPositive()`）。
- **更新 IDE 偵錯與測試範本**：更新 `.vscode/launch.json.example`，提供連線本機 `tibame_account_test` 資料庫進行隨選測試與偵錯之配置範本。

## Capabilities

### New Capabilities
<!-- 無新增獨立 Capability，本變更擴充並強化既有工程標準規格 -->

### Modified Capabilities
- `engineering-standards`: 新增本機 SQL Server 測試環境隔離、整合測試交易自動回滾基底，以及 SQL Server 主鍵動態斷言規範與 YAML 靜態驗證機制。

## Impact

- **受影響設定檔**：`src/test/resources/application-test-mssql.yml`（新建）、`.vscode/launch.json.example`（更新範本）。
- **受影響測試程式碼**：`src/test/java/com/tibame/integration/base/IntegrationTestBase.java`（新建）、`src/test/java/com/tibame/config/YamlConfigurationLintTest.java`（納入新設定檔掃描）。
- **業務核心代碼**：`src/main/java/**` 與 `src/main/resources/application-mssql.yml` 100% 零更動，業務邏輯完全解耦。
- **依賴與系統相容性**：零新增 Maven 依賴，相容現有 Spring Boot 3.3.13、Hibernate 6.5 與 Microsoft JDBC Driver。
