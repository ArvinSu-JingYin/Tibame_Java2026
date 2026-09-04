## Why

雖然專案已在先前的變更中修復了 YAML 中括號轉義並建立了強型別 `@ConfigurationProperties`，但由於 Spring Boot 的寬鬆綁定（Relaxed Binding）在執行期對未轉義的 YAML 映射鍵名與散裝 `@Value` 注入具有沉默容忍特性，且 IDE 診斷層級僅為提示（Hint），在日常敏捷開發中極易無意識地再次引入相同警告。

本變更依據 `docs/explorations/yaml_configuration_governance_and_prevention_strategy_exploration.md` 的規劃，落實防範復發的四道防禦縱深，透過引進機械化的自動測試守門機制（`YamlConfigurationLintTest` 與 `ConfigurationMetadataTest`），在 `mvn test` 建置階段自動攔截違規配置與缺失的元資料，達到零警告（Zero-Warning）的品質防護門禁。

## What Changes

- **新增 YAML 設定檔靜態掃描測試 (`YamlConfigurationLintTest`)**：使用 Spring Boot 內建之 SnakeYAML 解析器，自動掃描 `application.yml` 與 `application-test.yml`，針對 `logging.level.*`、`spring.jpa.properties.hibernate.*` 等 Map 節點檢驗鍵名是否遵循中括號 `["..."]` 轉義規範。
- **新增配置元資料生成檢驗測試 (`ConfigurationMetadataTest`)**：在測試階段驗證編譯期產出之 `target/classes/META-INF/spring-configuration-metadata.json` 是否存在，並斷言專案自訂前綴（如 `jwt`、`crypto.password.policy`）已正確收錄。
- **增修工程標準規範 (`engineering-standards`)**：將 YAML 設定檔語法守門測試與 Configuration Metadata 生成驗證列入 Zero-Warning Definition of Done (DoD) 的強制驗證項目。
- **更新 AI 代理規範與架構邊界指引**：修訂 `.agents/skills/spring-boot-skills/SKILL.md`，明定自訂設定項一律禁止散裝 `@Value` 注入，強制要求建立獨立 `@ConfigurationProperties` POJO。

## Capabilities

### New Capabilities
<!-- 無新增獨立 capability -->

### Modified Capabilities
- `engineering-standards`: 增訂設定檔自動化守門測試（YAML 語法掃描與配置元資料生成）之驗證情境與 Zero-Warning DoD 門禁要求。

## Impact

- **受影響測試組件**：新增 `src/test/java/com/tibame/config/YamlConfigurationLintTest.java` 與 `src/test/java/com/tibame/config/ConfigurationMetadataTest.java`。
- **受影響規範文件**：更新 `openspec/specs/engineering-standards/spec.md` 與 `.agents/skills/spring-boot-skills/SKILL.md`。
- **系統相依與執行期影響**：無任何生產代碼或執行期架構改動，不引入額外外部依賴（使用 Spring Boot Test 現有之 SnakeYAML 與 JUnit 5），建置流程依然維持 100% 綠燈與離線執行能力。
