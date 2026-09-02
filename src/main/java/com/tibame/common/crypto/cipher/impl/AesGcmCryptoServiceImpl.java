package com.tibame.common.crypto.cipher.impl;

import com.tibame.common.crypto.cipher.CryptoException;
import com.tibame.common.crypto.cipher.CryptoProperties;
import com.tibame.common.crypto.cipher.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 基於 AES-256-GCM 的抗量子對稱加解密服務實作
 * 具備自描述信封格式 ($v1$aes256gcm$...)、動態 IV 與 AEAD 防竄改完整性保護
 */
@Slf4j
@Service
public class AesGcmCryptoServiceImpl implements CryptoService {

    private static final String ALGORITHM_NAME = "AES-256-GCM";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ENVELOPE_PREFIX = "$v1$aes256gcm$";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesGcmCryptoServiceImpl(CryptoProperties properties) {
        if (properties == null || properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalArgumentException("AES-256-GCM 對稱加密金鑰不得為空");
        }
        this.secretKey = derive256BitKey(properties.getSecretKey());
        this.secureRandom = new SecureRandom();
        log.info("初始化 AES-256-GCM 抗量子對稱加密服務 (Key Size: 256-bit, IV Size: 12-byte, Tag: 128-bit)");
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String base64Iv = Base64.getUrlEncoder().withoutPadding().encodeToString(iv);
            String base64Cipher = Base64.getUrlEncoder().withoutPadding().encodeToString(cipherBytes);

            return ENVELOPE_PREFIX + base64Iv + "$" + base64Cipher;
        } catch (Exception e) {
            log.error("AES-256-GCM 加密失敗: {}", e.getMessage());
            throw new CryptoException("資料加密運算失敗", e);
        }
    }

    @Override
    public String decrypt(String cipherEnvelope) {
        if (cipherEnvelope == null || cipherEnvelope.isBlank()) {
            return cipherEnvelope;
        }

        if (!cipherEnvelope.startsWith(ENVELOPE_PREFIX)) {
            throw new CryptoException("不支援或損毀的密文信封格式: " + cipherEnvelope);
        }

        try {
            String payload = cipherEnvelope.substring(ENVELOPE_PREFIX.length());
            int splitIndex = payload.indexOf('$');
            if (splitIndex <= 0) {
                throw new CryptoException("密文信封結構無效，缺少 IV 或密文分段");
            }

            String base64Iv = payload.substring(0, splitIndex);
            String base64Cipher = payload.substring(splitIndex + 1);

            byte[] iv = Base64.getUrlDecoder().decode(base64Iv);
            byte[] cipherBytes = Base64.getUrlDecoder().decode(base64Cipher);

            if (iv.length != GCM_IV_LENGTH_BYTES) {
                throw new CryptoException("無效的 IV 長度: " + iv.length);
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (CryptoException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("AES-256-GCM 解密或防竄改驗證失敗: {}", e.getMessage());
            throw new CryptoException("密文解密失敗或資料已遭竄改", e);
        }
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    private SecretKey derive256BitKey(String rawSecret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支援 SHA-256 演算法", e);
        }
    }
}
