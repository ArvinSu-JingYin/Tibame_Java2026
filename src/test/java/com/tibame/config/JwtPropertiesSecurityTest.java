package com.tibame.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtProperties 資安態勢與啟動期防呆阻斷 (Fail-Fast Defense) 單元測試
 */
@DisplayName("JwtProperties 資安態勢驗證防線檢驗")
class JwtPropertiesSecurityTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("驗證預設開發金鑰符合 HMAC-SHA256 最小 32 位元組要求且於非 prod 環境可順暢啟動")
    void testDefaultDevSecretMeetsMinimumLengthRequirement() {
        MockEnvironment env = new MockEnvironment();
        JwtProperties properties = new JwtProperties(env);

        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        assertTrue(secretBytes.length >= 32, "預設金鑰字節長度應不小於 32 位元組 (256 位元)");
        assertEquals(JwtProperties.DEFAULT_DEV_SECRET.getBytes(StandardCharsets.UTF_8).length, secretBytes.length, "預設金鑰長度應與 DEFAULT_DEV_SECRET 一致");

        assertDoesNotThrow(properties::validateSecurityPosture,
                "在預設/開發環境下，預設金鑰應能順暢通過資安驗證");
    }

    @Test
    @DisplayName("驗證 prod Profile 啟用時若使用預設開發金鑰，立即拋出 IllegalStateException 中斷啟動")
    void testProductionStartupBlockedWithDefaultDevSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtProperties properties = new JwtProperties(env);
        properties.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

        assertThatThrownBy(properties::validateSecurityPosture)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("生產環境資安防護阻斷")
                .hasMessageContaining("prod")
                .hasMessageContaining("JWT_SECRET");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123456", "short_secret_key_under_32_bytes"})
    @DisplayName("驗證金鑰長度未滿 32 位元組 (256 位元) 時，立即拋出 IllegalArgumentException 拒絕弱金鑰")
    void testStartupBlockedWhenSecretLengthIsInsufficient(String weakSecret) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        JwtProperties properties = new JwtProperties(env);
        properties.setSecret(weakSecret);

        assertThatThrownBy(properties::validateSecurityPosture)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT 密鑰長度不足")
                .hasMessageContaining("32 位元組");
    }

    @Test
    @DisplayName("驗證金鑰為 null 時，立即拋出 IllegalArgumentException 拒絕空密鑰")
    void testStartupBlockedWhenSecretIsNull() {
        MockEnvironment env = new MockEnvironment();
        JwtProperties properties = new JwtProperties(env);
        properties.setSecret(null);

        assertThatThrownBy(properties::validateSecurityPosture)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT 密鑰長度不足");
    }

    @Test
    @DisplayName("驗證 prod Profile 下配置合法自訂高強度金鑰時，可順利通過安全校驗")
    void testProductionStartupSucceedsWithCustomSecureSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtProperties properties = new JwtProperties(env);
        String customSecureSecret = "CustomProductionSuperSecureSecretKeyWithSufficientEntropy2026!#SwissLedger";
        properties.setSecret(customSecureSecret);
        properties.setExpirationMs(7200000L);

        assertDoesNotThrow(properties::validateSecurityPosture);
        assertEquals(customSecureSecret, properties.getSecret());
        assertEquals(7200000L, properties.getExpirationMs());
    }

    @Test
    @DisplayName("驗證 Spring 容器載入與屬性覆蓋：透過自訂屬性成功綁定且通過啟動驗證")
    void testSpringContextBindingWithCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=CustomProductionSuperSecureSecretKeyWithSufficientEntropy2026!#SwissLedger",
                        "jwt.expiration-ms=3600000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JwtProperties props = context.getBean(JwtProperties.class);
                    assertThat(props.getSecret()).isEqualTo("CustomProductionSuperSecureSecretKeyWithSufficientEntropy2026!#SwissLedger");
                    assertThat(props.getExpirationMs()).isEqualTo(3600000L);
                });
    }

    @Test
    @DisplayName("驗證 Spring 容器在 prod Profile 下若未覆蓋預設金鑰，容器啟動失敗中斷")
    void testSpringContextFailsOnProductionWithDefaultSecret() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("生產環境資安防護阻斷");
                });
    }
}
