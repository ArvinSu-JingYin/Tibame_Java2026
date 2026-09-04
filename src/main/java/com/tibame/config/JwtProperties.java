package com.tibame.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JWT 權杖簽署與驗證配置屬性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 本機開發預設簽署金鑰 (72 位元組，僅限本機與測試環境使用)
     */
    public static final String DEFAULT_DEV_SECRET = "SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026";

    /**
     * HMAC-SHA256 安全密鑰最小長度門檻 (32 位元組 / 256 位元)
     */
    private static final int MIN_SECRET_BYTES = 32;

    @Autowired(required = false)
    private Environment environment;

    /**
     * JWT 簽署密鑰 (HMAC-SHA256 密鑰，建議長度至少 256 位元)
     */
    private String secret = DEFAULT_DEV_SECRET;

    /**
     * JWT 權杖有效存活時間 (毫秒)，預設為 86400000 毫秒 (24 小時)
     */
    private long expirationMs = 86400000L;

    public JwtProperties() {
    }

    public JwtProperties(Environment environment) {
        this.environment = environment;
    }

    /**
     * 啟動期安全性態勢校驗 (Fail-Fast 防線)
     * 1. 嚴格校驗金鑰字節長度是否達到 HMAC-SHA256 最低 32 位元組要求
     * 2. 在 prod Profile 啟用時嚴禁使用預設開發金鑰
     */
    @PostConstruct
    public void validateSecurityPosture() {
        byte[] secretBytes = (secret != null) ? secret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    String.format("JWT 密鑰長度不足：HMAC-SHA256 要求密鑰長度至少為 %d 位元組 (256 位元)，當前長度為 %d 位元組",
                            MIN_SECRET_BYTES, secretBytes.length));
        }

        if (isProdProfileActive() && DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "生產環境資安防護阻斷：檢測到啟用 prod Profile 但仍使用預設開發 JWT 金鑰。請設定環境變數 JWT_SECRET 以提供高強度生產密鑰！");
        }
    }

    private boolean isProdProfileActive() {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }
}
