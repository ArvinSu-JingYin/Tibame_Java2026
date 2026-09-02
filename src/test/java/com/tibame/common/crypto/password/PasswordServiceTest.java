package com.tibame.common.crypto.password;

import com.tibame.common.crypto.password.impl.BCryptPasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordService 單元測試")
public class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new BCryptPasswordServiceImpl(10);
    }

    @Test
    @DisplayName("測試正常密碼雜湊與驗證成功")
    void testHashAndVerifySuccess() {
        String rawPassword = "SecurePassword2026!";
        String hash = passwordService.hash(rawPassword);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$10$"));
        assertTrue(passwordService.verify(rawPassword, hash));
    }

    @Test
    @DisplayName("測試錯誤密碼驗證失敗")
    void testVerifyFailureOnWrongPassword() {
        String rawPassword = "SecurePassword2026!";
        String hash = passwordService.hash(rawPassword);

        assertFalse(passwordService.verify("WrongPassword123", hash));
        assertFalse(passwordService.verify(null, hash));
        assertFalse(passwordService.verify(rawPassword, null));
        assertFalse(passwordService.verify(rawPassword, ""));
    }

    @Test
    @DisplayName("測試 needsUpgrade 升級檢測邏輯")
    void testNeedsUpgrade() {
        // 符合目前 strength 10 的 hash -> false
        String currentHash = passwordService.hash("testPassword");
        assertFalse(passwordService.needsUpgrade(currentHash));

        // 舊版或較低 cost 的 hash (例如 cost 04) -> true
        String lowCostHash = "$2a$04$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        assertTrue(passwordService.needsUpgrade(lowCostHash));

        // 非 BCrypt 格式或空值 -> true
        assertTrue(passwordService.needsUpgrade("plain-text-legacy-md5-or-sha1"));
        assertTrue(passwordService.needsUpgrade(null));
        assertTrue(passwordService.needsUpgrade(""));
    }

    @Test
    @DisplayName("測試空密碼雜湊拋出例外")
    void testHashEmptyPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> passwordService.hash(null));
        assertThrows(IllegalArgumentException.class, () -> passwordService.hash(""));
    }
}
