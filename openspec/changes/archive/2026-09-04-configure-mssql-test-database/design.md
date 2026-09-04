## Context

本專案持久層使用 Spring Data JPA 與 Hibernate 6.5，底層資料庫在正式/開發環境採用 Microsoft SQL Server，CI/PR 自動化驗證環境採用 H2 記憶體資料庫。現工程師已於本機 SQL Server 實例手動建立了 `tibame_account_test` 資料庫。設計重點在於提供零侵入、零污染且可隨選啟用的真機測試架構。詳細背景與影響性分析請參閱 [mssql_test_database_integration_and_impact_exploration.md](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/docs/explorations/mssql_test_database_integration_and_impact_exploration.md) 與 `proposal.md`。

## Goals / Non-Goals

**Goals:**
- 提供專屬 `test-mssql` Profile 設定檔，連線本機 `tibame_account_test` 資料庫，並與日常開發庫 `tibame_account` 嚴格隔離。
- 提供開箱即用的自動 DDL 初始化（`schema.sql` 與 `data.sql`），達成新庫零手動建表負擔。
- 提供 `IntegrationTestBase` 抽象測試基底，預設宣告 `@Transactional` 確保測試資料自動回滾。
- 建立 SQL Server `IDENTITY` 動態主鍵斷言規範，杜絕測試跳號脆弱性。
- 將 `application-test-mssql.yml` 納入 `YamlConfigurationLintTest` 驗證，遵循 Map Key 括號轉義規範。
- 更新 `.vscode/launch.json.example`，提供連線測試庫進行除錯的範本。

**Non-Goals:**
- 不修改任何業務核心代碼（`src/main/java/**`）與正式設定（`src/main/resources/application-mssql.yml`）。
- 不改變 CI/PR 預設流程（CI 雲端環境依賴預設 `test` Profile 與 H2，不強制要求 SQL Server 實體服務）。
- 不引入重型資料庫遷移工具（如 Flyway 或 Liquibase），維持 Spring Boot 輕量級 SQL 初始化機制。

## Decisions

### 決策 1：採獨立 Profile (`application-test-mssql.yml`) 而非覆寫 `application-test.yml`
- **架構方案**：預設 `test` Profile 專注於 H2 記憶體模式（支援 CI/PR 5 秒極速反饋）；`test-mssql` Profile 專門用於本機隨選執行真機驗收與 SQL Server 方言除錯。
- **評估與替代方案**：若直接在命令列使用 `-Dspring.datasource.url=...` 覆寫，將遺漏 Hibernate 方言、SQL 初始化模式與特定日誌等級設定，維護困難。獨立 Profile 具備宣告式管理與版本受控優勢。

### 決策 2：透過 `spring.sql.init.mode: always` 達成零手動 DDL 初始化
- **架構方案**：測試設定檔宣告 `spring.sql.init.mode: always`，搭配 `continue-on-error: true`，直接調用現有具備 `IF NOT EXISTS` 冪等防護之 `schema.sql` 與 `data.sql`。
- **評估與替代方案**：若要求開發者開啟 SSMS 手動建表，易因人為遺漏導致環境不一致；若依賴 `ddl-auto: create-drop`，則無法載入系統預設種子分類資料。結合 SQL 初始化與 `ddl-auto: update` 為最佳實踐。

### 決策 3：建立 `IntegrationTestBase` 並以 `@Transactional` 達成狀態自動回滾
- **架構方案**：測試案例繼承 `IntegrationTestBase`，Spring TestContext 自動在每個測試方法外層套用事務，測試結束後一律 Rollback，測試庫永遠保持初始乾淨狀態。
- **評估與替代方案**：使用 `@DirtiesContext` 會導致 Spring Context 重建、測試速度急遽下降；在 `@AfterEach` 手動編寫 `DELETE FROM ...` 容易因外鍵條件約束失敗或遺漏表格。`@Transactional` 回滾成本最低且速度最快。

### 決策 4：強制採用動態 ID 斷言相容 SQL Server `IDENTITY` 跳號
- **架構方案**：在測試案例中，嚴禁 `assertEquals(1L, saved.getId())`，一律使用 `assertThat(saved.getId()).isNotNull().isPositive()` 並將回傳之動態 ID 傳遞給後續操作。
- **評估與替代方案**：SQL Server 在交易回滾時其已耗用的自增 `IDENTITY` 值不會回退，硬編碼 ID 將導致後續測試必定失敗。動態斷言具備 100% 容錯性與可移植性。

### 決策 5：嚴格遵循 YAML Map Key 括號轉義規範
- **架構方案**：在 `application-test-mssql.yml` 中，包含點號之鍵值一律使用 `"[org.hibernate.SQL]"`、`"[format_sql]"` 等括號語法，並擴充 `YamlConfigurationLintTest` 將此檔案納入驗證清單。
- **評估與替代方案**：不轉義將觸發 Spring Boot Relaxed Binding 混淆警告與 IDE/測試檢查失敗。

## Risks / Trade-offs

- **[風險：本機未啟動 SQL Server 實例即執行 `test-mssql`]**  
  → **緩解策略**：預設 `mvn test` 依舊維持 H2 記憶體模式；僅在明確傳入 `-Dspring.profiles.active=test-mssql` 時連線實體庫，並在文檔與 IDE 設定中清楚提示資料庫必須已啟動。
- **[風險：初始種子資料重複執行引發主鍵衝突]**  
  → **緩解策略**：`schema.sql` 與 `data.sql` 已內建 `IF NOT EXISTS` 防護，且測試檔設定 `continue-on-error: true`，確保多次開機依然冪等安全。
