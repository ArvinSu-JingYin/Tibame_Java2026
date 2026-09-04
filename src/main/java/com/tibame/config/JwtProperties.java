package com.tibame.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 權杖簽署與驗證配置屬性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 簽署密鑰 (HMAC-SHA256 密鑰，建議長度至少 256 位元)
     */
    private String secret = "SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026";

    /**
     * JWT 權杖有效存活時間 (毫秒)，預設為 86400000 毫秒 (24 小時)
     */
    private long expirationMs = 86400000L;
}
