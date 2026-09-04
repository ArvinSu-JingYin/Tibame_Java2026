package com.tibame.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot Configuration Metadata 編譯產出檢驗測試
 * 驗證 spring-boot-configuration-processor 於編譯期正確產出自訂屬性元資料
 */
@DisplayName("配置元資料編譯產出檢驗")
class ConfigurationMetadataTest {

    private static final Path METADATA_PATH = Path.of("target/classes/META-INF/spring-configuration-metadata.json");

    @Test
    @DisplayName("驗證編譯期產出 spring-configuration-metadata.json 且包含專案自訂配置前綴")
    void testConfigurationMetadataExistsAndContainsCustomPrefixes() throws Exception {
        assertTrue(Files.exists(METADATA_PATH),
                "找不到 target/classes/META-INF/spring-configuration-metadata.json，請確認已執行編譯 (mvn compile / mvn test-compile)");

        String metadataContent = Files.readString(METADATA_PATH);

        assertTrue(metadataContent.contains("\"name\": \"jwt\"") || metadataContent.contains("\"name\": \"jwt.secret\""),
                "Metadata 必須包含 jwt 相關配置群組或屬性 (如 JwtProperties)");

        assertTrue(metadataContent.contains("\"name\": \"crypto.password.policy\"") || metadataContent.contains("crypto.password.policy"),
                "Metadata 必須包含密碼原則 crypto.password.policy 相關配置 (如 PasswordPolicyProperties)");

        assertTrue(metadataContent.contains("\"name\": \"crypto.cipher\"") || metadataContent.contains("crypto.cipher"),
                "Metadata 必須包含加解密演算法 crypto.cipher 相關配置 (如 CryptoProperties)");
    }
}
