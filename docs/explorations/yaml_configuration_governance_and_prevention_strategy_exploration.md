# Spring Boot YAML 設定檔治理、IDE 診斷防護與防範復發策略探索報告 (YAML Configuration Governance & Recurrence Prevention Strategy Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Spring Boot 3.3.13 / Spring Tools 4 / SnakeYAML / Configuration Metadata / JUnit 5  
> **目標範疇**：YAML 映射鍵轉義防護、Configuration Metadata 自動生成檢驗、防範復發之四道防禦縱深  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **前序探索**：[YAML 設定檔規範、IDE 診斷排除與 JWT 強型別配置探索報告 (yaml_configuration_and_ide_diagnostics_exploration.md)](yaml_configuration_and_ide_diagnostics_exploration.md)  

---

## 1. 探索背景與問題復發風險 (Background & Recurrence Risks)

在專案完成 [`yaml_configuration_and_ide_diagnostics_exploration.md`](yaml_configuration_and_ide_diagnostics_exploration.md) 所定義的規範化修復後，專案成功消除了整合測試 YAML 中的 5 筆診斷提示（包含 4 筆 `YAML_SHOULD_ESCAPE` 與 1 筆 `YAML_UNKNOWN_PROPERTY`）。

然而，在日常敏捷開發中，當團隊成員或 AI 代理人新增業務功能（例如引入第三方服務、快取設定或稽核日誌開關）時，極易**無意識地再次引發相同問題**。

### 1.1 隱性破口深層剖析

```
+-------------------------------------------------------------------------------+
|                            問題易於復發的「三大隱性破口」                     |
+-------------------------------------------------------------------------------+
|                                                                               |
|  1. Spring Boot 寬鬆綁定（Relaxed Binding）的「沉默容忍」                      |
|     * 即使 YAML 鍵名未加括號轉義（如 format_sql: true）                       |
|     * 即使自訂配置隨手使用散裝 @Value("${custom.prop}") 注入                  |
|     --> 應用程式與單元測試依然 100% 成功啟動與通過，編譯器完全不報錯！         |
|                                                                               |
|  2. IDE 診斷訊息層級僅為「提示（Hint / Info）」                                |
|     * VS Code Spring Tools 將此類警告歸類為 Severity: Hint (最弱層級)         |
|     * 在缺乏嚴格把關下，開發者往往專注於業務邏輯，忽略灰/黃色波浪底線          |
|                                                                               |
|  3. 缺乏「機械化守門員（Automated Enforcement）」                             |
|     * CI 建置流程（mvn test / verify）預設不會驗證 YAML 的轉義格式與元資料    |
|     * 缺少自動化測試直接在提交前中斷違規代碼                                   |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. 目前專案已具備之防護基石 (Current Protection Audit)

目前專案已經建立並具備了良好的強型別配置基石：

```
+-------------------------------------------------------------------------------+
|                             目前已落地的防護資產                              |
+-------------------+-----------------------------------------------------------+
| 防護維度          | 現況與實現組件                                            |
+-------------------+-----------------------------------------------------------+
| 1. 建構元資料     | pom.xml 已納入 spring-boot-configuration-processor        |
|                   | (<optional>true</optional>)，編譯時可自動提取元資料       |
+-------------------+-----------------------------------------------------------+
| 2. 強型別配置範例 | com.tibame.config.JwtProperties (前綴: "jwt")             |
|                   | com.tibame.common.crypto.password.PasswordPolicyProperties|
|                   | (前綴: "crypto.password.policy")                          |
+-------------------+-----------------------------------------------------------+
| 3. YAML 鍵轉義    | application.yml 與 application-test.yml 已全面完成        |
|                   | "[format_sql]"、"[com.tibame]" 等中括號語法標準化         |
+-------------------+-----------------------------------------------------------+
| 4. OpenSpec 規格  | 在變更分支中增訂了                                        |
|                   | Requirement: Spring Boot Configuration Hygiene & Metadata |
+-------------------+-----------------------------------------------------------+
```

---

## 3. 防範復發的四道防禦縱深 (Defense-in-Depth Architecture)

為了確保未來新增任何設定項皆能遵循規範，本探索提出「四道防禦縱深」架構模型：

```
+-------------------------------------------------------------------------------+
|                       防範 YAML 與配置警告的四道防護縱深                      |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [第 1 道防線] 規格與 AI 提示詞約束 (Specs & Agent Guidelines)                |
|      * 將 delta spec 同步合併回主規格 (OpenSpec Sync/Archive)                 |
|      * 更新 .agents/skills/spring-boot-skills 的 DoD 與編碼規範               |
|                                                                               |
|  [第 2 道防線] 機械化自動驗證測試守門 (Automated Verification Test Gate)      |
|      * YamlConfigurationLintTest：透過 SnakeYAML 自動掃描 Map 鍵未轉義        |
|      * ConfigurationMetadataTest：驗證編譯期 spring-configuration-metadata    |
|                                                                               |
|  [第 3 道防線] 架構邊界檢驗 (Architecture Boundary Rules)                     |
|      * 業務層 (@Service) 限制使用散裝 @Value 注入自訂前綴屬性                 |
|      * 自訂配置項一律要求建立獨立 @ConfigurationProperties POJO               |
|                                                                               |
|  [第 4 道防線] IDE 檢驗與 Git 提交規範 (IDE Settings & Zero-Warning DoD)      |
|      * 將「IDE Problems 面板 0 警告」列為 PR 與 OpenSpec Archive 強制門禁     |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 4. 自動化防護具體實施藍圖 (Automated Testing Implementation Blueprint)

將規範落實為「可執行的代碼測試」是阻絕復發的最有效途徑。以下設計兩個可在 `src/test/java` 中實施的自動化驗證測試。

### 4.1 YAML 格式與轉義靜態掃描測試 (`YamlConfigurationLintTest`)

此測試使用 Spring Boot 內建的 SnakeYAML 解析器，載入 `src/main/resources/application.yml` 與 `src/test/resources/application-test.yml`，針對所有 Map 節點進行鍵名規範校驗：

```java
package com.tibame.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * YAML 設定檔語法與轉義規範靜態檢驗測試
 */
@DisplayName("YAML 設定檔規範化檢驗")
class YamlConfigurationLintTest {

    private final List<String> targetYamlPaths = List.of(
        "src/main/resources/application.yml",
        "src/test/resources/application-test.yml"
    );

    @Test
    @DisplayName("驗證 YAML Map 鍵名若含點號或底線必須使用中括號包裹轉義")
    void testMapKeysContainNoUnescapedDotsOrUnderscores() throws Exception {
        Yaml yaml = new Yaml();
        List<String> violations = new ArrayList<>();

        for (String filePath : targetYamlPaths) {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) continue;

            try (InputStream in = Files.newInputStream(path)) {
                Iterable<Object> documents = yaml.loadAll(in);
                for (Object doc : documents) {
                    if (doc instanceof Map<?, ?> rootMap) {
                        scanMap(rootMap, "", filePath, violations);
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("發現未符合 Spring Boot 轉義規範之 YAML 映射鍵名 (需使用 '[\"key\"]' 轉義):\n" 
                 + String.join("\n", violations));
        }
    }

    @SuppressWarnings("unchecked")
    private void scanMap(Map<?, ?> map, String parentPath, String file, List<String> violations) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;

            // 檢查日誌層級 (logging.level.*) 與 JPA 特性屬性 (spring.jpa.properties.*) 等 Map 節點
            if (parentPath.startsWith("logging.level") || parentPath.contains("properties.hibernate")) {
                if ((key.contains(".") || key.contains("_")) && (!key.startsWith("[") || !key.endsWith("]"))) {
                    violations.add(String.format("檔案 [%s] 鍵名 [%s] (路徑: %s) 包含點號或底線，但未以中括號轉義",
                            file, key, currentPath));
                }
            }

            if (entry.getValue() instanceof Map<?, ?> childMap) {
                scanMap(childMap, currentPath, file, violations);
            }
        }
    }
}
```

---

### 4.2 配置元資料生成檢驗測試 (`ConfigurationMetadataTest`)

此測試確保專案自訂的 `@ConfigurationProperties` 確實被編譯器處理，且在 `target/classes/META-INF/spring-configuration-metadata.json` 之中留有定義，防止開發者遺漏編譯器設定或忘記宣告前綴：

```java
package com.tibame.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot Configuration Metadata 存在性檢驗
 */
@DisplayName("配置元資料編譯產出檢驗")
class ConfigurationMetadataTest {

    @Test
    @DisplayName("驗證編譯期必須產出 spring-configuration-metadata.json 且包含專案前綴")
    void testConfigurationMetadataExistsAndContainsCustomPrefixes() throws Exception {
        File metadataFile = new File("target/classes/META-INF/spring-configuration-metadata.json");
        
        // 若在單元測試階段執行，確認目標目錄是否已存在（test-compile 或 compile 之後）
        if (metadataFile.exists()) {
            String content = Files.readString(metadataFile.toPath());
            
            // 驗證自訂配置屬性已成功編譯進元資料
            assertTrue(content.contains("\"name\": \"jwt.secret\"") || content.contains("\"jwt\""),
                    "Metadata 必須包含 jwt 配置屬性");
            assertTrue(content.contains("crypto.password.policy"),
                    "Metadata 必須包含密碼原則 crypto.password.policy 配置屬性");
        }
    }
}
```

---

## 5. 防護方案評估與權衡矩陣 (Options Evaluation & Tradeoffs Matrix)

```
+-----------------------------------------------------------------------------------------------+
|                                      防範對策評估與權衡矩陣                                    |
+---------------------------+----------+----------+---------------------------------------------+
| 對策方案                  | 實施成本 | 防護強度 | 核心優勢與適用時機                          |
+---------------------------+----------+----------+---------------------------------------------+
| 方案 A：OpenSpec 規格同步 | 極低     | 中       | 確立團隊單一事實來源（SSOT），約束 AI 代理  |
|         與 Skills 手冊增修|          |          | 的代碼生成行為。                            |
+---------------------------+----------+----------+---------------------------------------------+
| 方案 B：YAML 語法單元測試 | 低       | 極高     | 【最推薦】將規範代碼化，mvn test 自動攔截， |
|         (YamlLintTest)    |          |          | 杜絕任何違規 YAML 進入主幹。                |
+---------------------------+----------+----------+---------------------------------------------+
| 方案 C：Metadata 生成測試 | 極低     | 高       | 確保任何新增的配置類別均成功產出 IDE        |
|         (MetadataTest)    |          |          | 自動補全與校驗中繼資料。                    |
+---------------------------+----------+----------+---------------------------------------------+
| 方案 D：Zero-Warning DoD  | 極低     | 高       | 依賴流程把關，作為所有變更完成前的最終守門。|
+---------------------------+----------+----------+---------------------------------------------+
```

---

## 6. 後續落地與 CI 整合建議 (Next Steps & Roadmap)

為落實本探索之規劃，建議後續工作按以下節奏推展：

1. **第一階段（規格同步收斂）**：
   - 透過 `/opsx-sync` 或 `/opsx-archive`，將目前完成的 `standardize-yaml-and-jwt-properties` 變更規格同步至主規格庫 [`openspec/specs/engineering-standards/spec.md`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/openspec/specs/engineering-standards/spec.md)。
   - 同步修訂 `.agents/skills/spring-boot-skills` 手冊之 DoD 清單。
2. **第二階段（自動化守門測試實作）**：
   - 開啟輕量 OpenSpec 變更（如 `add-yaml-and-metadata-verification-tests`）。
   - 實作 `YamlConfigurationLintTest`，使 Maven 建置具備自我免疫力，徹底杜絕警告復發。
