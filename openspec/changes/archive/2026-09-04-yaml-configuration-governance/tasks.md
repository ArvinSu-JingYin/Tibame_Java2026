## 1. 自動化驗證測試實作 (Automated Verification Test Gate)

- [x] 1.1 實作 YAML 格式與轉義靜態掃描測試 `YamlConfigurationLintTest`，驗證 `application.yml` 與 `application-test.yml` 之 Map 鍵名（如 `logging.level.*` 與 `properties.hibernate.*`）符合中括號轉義規範，並透過 `mvn test -Dtest=YamlConfigurationLintTest` 驗證通過
- [x] 1.2 實作配置元資料生成檢驗測試 `ConfigurationMetadataTest`，驗證 `target/classes/META-INF/spring-configuration-metadata.json` 存在且包含自訂屬性前綴（`jwt`、`crypto.password.policy`），並透過 `mvn test -Dtest=ConfigurationMetadataTest` 驗證通過

## 2. 規範與代理指引更新 (Standards and Skills Update)

- [x] 2.1 更新 `.agents/skills/spring-boot-skills/SKILL.md`，納入配置治理四道防線、Zero-Warning DoD 檢查清單與禁止散裝 `@Value` 注入自訂前綴之架構邊界規範
- [x] 2.2 執行全專案單元測試驗證，透過 `mvn test` 確保全數測試 100% 通過且 IDE 問題面板維持 0 錯誤 0 警告
