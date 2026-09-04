package com.tibame.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * YAML 設定檔語法與轉義規範靜態檢驗測試
 * 確保 logging.level.* 與 properties.hibernate.* 等階層式 Map 鍵名具備中括號轉義
 */
@DisplayName("YAML 設定檔規範化檢驗")
class YamlConfigurationLintTest {

    private final List<String> targetYamlPaths = List.of(
        "src/main/resources/application.yml",
        "src/main/resources/application-mssql.yml",
        "src/test/resources/application-test.yml",
        "src/test/resources/application-test-mssql.yml"
    );

    @Test
    @DisplayName("驗證各環境 YAML 設定檔皆符合中括號轉義規範")
    void testTargetYamlFilesComplyWithEscapingRules() throws Exception {
        Yaml yaml = new Yaml();
        List<String> violations = new ArrayList<>();

        for (String filePath : targetYamlPaths) {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                continue;
            }

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

    @Test
    @DisplayName("驗證掃描器可有效偵測未轉義之 Map 鍵名違規")
    void testScannerCatchesUnescapedKeyViolations() {
        List<String> violations = new ArrayList<>();
        Map<String, Object> mockYamlMap = Map.of(
            "logging", Map.of(
                "level", Map.of(
                    "com.tibame", "DEBUG", // 未轉義點號
                    "[org.hibernate.SQL]", "INFO" // 已轉義
                )
            ),
            "spring", Map.of(
                "jpa", Map.of(
                    "properties", Map.of(
                        "hibernate", Map.of(
                            "format_sql", true // 未轉義底線
                        )
                    )
                )
            )
        );

        scanMap(mockYamlMap, "", "mock.yml", violations);

        assertFalse(violations.isEmpty(), "掃描器應檢出未轉義違規");
        assertTrue(violations.stream().anyMatch(v -> v.contains("com.tibame")), "應檢出 com.tibame 未轉義");
        assertTrue(violations.stream().anyMatch(v -> v.contains("format_sql")), "應檢出 format_sql 未轉義");
        assertFalse(violations.stream().anyMatch(v -> v.contains("org.hibernate.SQL")), "不應將已轉義鍵判定為違規");
    }

    private void scanMap(Map<?, ?> map, String parentPath, String file, List<String> violations) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;

            // 檢查日誌層級 (logging.level.*) 與 JPA 特性屬性 (properties.hibernate.*) 等 Map 節點
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
