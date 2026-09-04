## Context

本設計落實 `docs/explorations/yaml_configuration_governance_and_prevention_strategy_exploration.md` 所規劃之防範復發防禦縱深。目前專案已完成 YAML 中括號轉義修復與強型別配置 POJO（`JwtProperties`、`PasswordPolicyProperties`），並在 `pom.xml` 引入了 `spring-boot-configuration-processor`。然而，缺乏機械化自動驗證門禁將導致未來新增業務配置時，容易再度發生 `YAML_SHOULD_ESCAPE` 與 `YAML_UNKNOWN_PROPERTY` 診斷警告。詳細動機請參見 `proposal.md`。

## Goals / Non-Goals

**Goals:**
- 建立 `YamlConfigurationLintTest`，在單元測試（`mvn test`）階段以毫秒級速度解析 `application.yml` 與 `application-test.yml`，自動攔截未依中括號轉義的 Map 鍵名（如 `logging.level.*` 與 `properties.hibernate.*`）。
- 建立 `ConfigurationMetadataTest`，檢驗編譯期 `target/classes/META-INF/spring-configuration-metadata.json` 產出與專案自訂屬性註冊情況。
- 零外部依賴擴充：完全使用 Spring Boot 內建之 SnakeYAML 與 JUnit 5，保持純離線（No-CDN / Offline）架構與純粹的建置流程。
- 將設定治理防線納入 `.agents/skills/spring-boot-skills` 與專案 DoD，規範自訂配置嚴格禁止散裝 `@Value` 注入，一律採用 `@ConfigurationProperties`。

**Non-Goals:**
- 不引入重型的第三方靜態代碼分析 Maven 外掛（如自訂 Checkstyle 外掛或額外 Gradle/NPM 工具）。
- 不對所有標準 Spring Boot kebab-case 屬性進行全域強制括號（僅鎖定特定階層式 Map 節點，避免誤報）。

## Decisions

### 1. 採用 SnakeYAML 單元測試守門而非 Maven 驗證外掛
- **決策**：在 `src/test/java/com/tibame/config/YamlConfigurationLintTest.java` 實作靜態掃描測試。
- **理由**：
  - SnakeYAML 已隨 `spring-boot-starter-test` 引入專案，無須調整 `pom.xml` 依賴。
  - 單元測試在日常開發與 CI 中自動由 `mvn test` 觸發，反饋速度極快（< 50ms），一旦違規直接中斷建置。
- **替代方案**：
  - *Maven Checkstyle / Antlr 外掛*：配置複雜、依賴下載容易受離線環境影響。
  - *Raw Regex 行文本比對*：易受註解、空行、字串引號干擾，準確度不及語法樹解析。

### 2. 聚焦掃描階層式 Map 節點（針對性防護）
- **決策**：針對 `logging.level` 及 `properties.hibernate` 等 Map 型態父節點進行鍵名深度遍歷，凡含點號 `.` 或底線 `_` 且未以 `[` 開頭、`]` 結尾者視為違規。
- **理由**：Spring Boot 寬鬆綁定在處理 Map 集合時，僅有此類動態鍵名需要中括號轉義以避免鍵名被拆解。其他標準 Bean 屬性（如 `spring.datasource.url`）不受此限。

### 3. 配置元資料檔案存在性與關鍵屬性檢驗
- **決策**：透過 `ConfigurationMetadataTest` 驗證 `target/classes/META-INF/spring-configuration-metadata.json` 的產生，並檢查 `jwt` 與 `crypto.password.policy` 前綴。
- **理由**：確保註解處理器在編譯流程中持續生效，防範開發者新增屬性類別時遺漏 `@ConfigurationProperties` 或套件路徑未被掃描。

## Risks / Trade-offs

- **[Risk] IDE 單獨執行特定測試時尚未執行編譯處理**
  - **Mitigation**: `ConfigurationMetadataTest` 在檔案不存在時（如僅執行單一測試且 clean 後未 compile），應給予合理防護或提示需先完成 `mvn test-compile`，避免誤判。
- **[Risk] 未來新增其他 Profile 設定檔（如 application-prod.yml）**
  - **Mitigation**: 在 `YamlConfigurationLintTest` 中集中定義掃描清單 `targetYamlPaths`，或動態搜尋 `resources` 目錄下所有 `application*.yml` 檔案。

## Migration Plan

1. 新增測試類別 `YamlConfigurationLintTest` 與 `ConfigurationMetadataTest`。
2. 執行 `mvn test` 驗證現有 YAML 與 Metadata 符合規範（100% 綠燈）。
3. 同步更新 `.agents/skills/spring-boot-skills/SKILL.md` 之 DoD 與配置規範說明。
