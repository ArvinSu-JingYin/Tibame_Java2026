## 1. 測試組態與靜態驗證 (Configuration & Linting)

- [x] 1.1 建立專屬測試設定檔 `src/test/resources/application-test-mssql.yml`，配置連線 `tibame_account_test`、`spring.sql.init.mode: always`、`continue-on-error: true` 及中括號轉義鍵名，並檢查檔案存在且語法正確
- [x] 1.2 擴充 `YamlConfigurationLintTest.java` 將 `application-test-mssql.yml` 納入掃描清單，並執行 `./mvnw test -Dtest=YamlConfigurationLintTest` 驗證測試綠燈通過

## 2. 測試基底與環境範本 (Testing Base & IDE Tooling)

- [x] 2.1 建立整合測試共用基底類別 `src/test/java/com/tibame/integration/base/IntegrationTestBase.java`，標註 `@SpringBootTest`、`@AutoConfigureMockMvc` 與 `@Transactional`，並執行 `./mvnw test-compile` 驗證編譯成功
- [x] 2.2 更新 `.vscode/launch.json.example` 新增連線 `tibame_account_test` 資料庫之隨選測試與偵錯配置範本，並驗證 JSON 格式無語法錯誤

## 3. 全量測試與防禦標準驗證 (Verification & Defense Gate)

- [x] 3.1 執行全量單元測試 `./mvnw test`，驗證既有測試與新靜態檢驗 100% 通過且零警告
- [x] 3.2 驗證本機 SQL Server 真機整合測試指令，確認以 `test-mssql` Profile 執行測試時可正確連線並在測試結束後自動回滾
