package com.tibame.common.crypto.cipher;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通用加解密配置類
 */
@Data
@Component
@ConfigurationProperties(prefix = "crypto.cipher")
public class CryptoProperties {

    /**
     * 對稱加密主金鑰（建議 256-bit / 32 字元以上字串或 Base64 字串）
     */
    private String secretKey = "SwissLedgerQuantumSafeAes256GcmKey2026!#SwissLedger2026AESKey";

    /**
     * 預設加解密演算法
     */
    private String algorithm = "AES-256-GCM";
}
