package com.tibame.common.crypto.cipher;

import com.tibame.common.crypto.cipher.impl.AesGcmCryptoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CryptoService (AES-256-GCM) 單元測試")
public class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties();
        properties.setSecretKey("MyCustomSecureQuantumSafeKey2026!#TestingSecret");
        cryptoService = new AesGcmCryptoServiceImpl(properties);
    }

    @Test
    @DisplayName("測試正常加解密還原明文（包含繁體中文與符號）")
    void testEncryptAndDecryptSuccess() {
        String plainText = "2026年每日記帳系統：私密帳戶餘額 NT$ 8,888,888 與 敏感備註 #SwissStyle";
        String cipherEnvelope = cryptoService.encrypt(plainText);

        assertNotNull(cipherEnvelope);
        assertTrue(cipherEnvelope.startsWith("$v1$aes256gcm$"));

        String decrypted = cryptoService.decrypt(cipherEnvelope);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("測試隨機 IV 確保語意安全（相同明文產生不同密文）")
    void testRandomIvProducesDifferentCiphertexts() {
        String plainText = "SameSecretData123";
        String cipher1 = cryptoService.encrypt(plainText);
        String cipher2 = cryptoService.encrypt(plainText);

        assertNotEquals(cipher1, cipher2);
        assertEquals(plainText, cryptoService.decrypt(cipher1));
        assertEquals(plainText, cryptoService.decrypt(cipher2));
    }

    @Test
    @DisplayName("測試密文遭竄改時觸發 AEAD 防竄改驗證並拋出 CryptoException")
    void testTamperedCiphertextThrowsCryptoException() {
        String plainText = "SensitiveFinancialRecord";
        String cipherEnvelope = cryptoService.encrypt(plainText);

        // 竄改密文中的有效字元 (避免落在 Base64 未使用的填充位元)
        int tamperIndex = cipherEnvelope.length() - 3;
        char origChar = cipherEnvelope.charAt(tamperIndex);
        char tamperedChar = (origChar == 'A') ? 'B' : 'A';
        String tamperedCipher = cipherEnvelope.substring(0, tamperIndex) + tamperedChar + cipherEnvelope.substring(tamperIndex + 1);

        assertThrows(CryptoException.class, () -> cryptoService.decrypt(tamperedCipher));
    }

    @Test
    @DisplayName("測試無效信封格式拋出 CryptoException")
    void testInvalidEnvelopeFormatThrowsCryptoException() {
        assertThrows(CryptoException.class, () -> cryptoService.decrypt("not-a-valid-envelope"));
        assertThrows(CryptoException.class, () -> cryptoService.decrypt("$v1$aes256gcm$missingSplit"));
        assertThrows(CryptoException.class, () -> cryptoService.decrypt("$v2$unsupported$iv$payload"));
    }

    @Test
    @DisplayName("測試 null 與空字串邊界條件")
    void testNullAndEmptyHandling() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.decrypt(null));
        assertEquals("", cryptoService.decrypt(""));
    }
}
