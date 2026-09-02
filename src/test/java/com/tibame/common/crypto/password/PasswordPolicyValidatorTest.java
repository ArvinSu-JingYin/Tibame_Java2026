package com.tibame.common.crypto.password;

import com.tibame.common.crypto.password.impl.DefaultPasswordPolicyValidator;
import com.tibame.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordPolicyValidator 單元測試")
public class PasswordPolicyValidatorTest {

    private PasswordPolicyProperties properties;
    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        properties = new PasswordPolicyProperties();
        properties.setMinLength(6);
        properties.setMaxLength(32);
        properties.setRequireDigit(true);
        properties.setRequireUppercase(true);
        properties.setRequireLowercase(true);
        properties.setRequireSpecialChar(true);

        validator = new DefaultPasswordPolicyValidator(properties);
    }

    @Test
    @DisplayName("符合原則的密碼通過校驗")
    void testValidPassword() {
        String compliantPassword = "Pass123!word";
        assertTrue(validator.isValid(compliantPassword));
        assertDoesNotThrow(() -> validator.validate(compliantPassword));
    }

    @Test
    @DisplayName("過短或過長的密碼校驗失敗")
    void testLengthViolations() {
        assertFalse(validator.isValid("P1!a")); // 長度 4 < 6
        assertThrows(ApiException.class, () -> validator.validate("P1!a"));

        String tooLong = "A1!" + "a".repeat(40);
        assertFalse(validator.isValid(tooLong));
        assertThrows(ApiException.class, () -> validator.validate(tooLong));
    }

    @Test
    @DisplayName("缺少必要字符組合校驗失敗")
    void testMissingCharacterTypes() {
        // 缺少大寫
        assertFalse(validator.isValid("pass123!word"));
        // 缺少數字
        assertFalse(validator.isValid("Password!word"));
        // 缺少特殊符號
        assertFalse(validator.isValid("Password123"));

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate("Password123"));
        assertTrue(ex.getMessage().contains("必須包含至少一個特殊符號"));
    }

    @Test
    @DisplayName("空密碼校驗失敗")
    void testEmptyPassword() {
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
        assertThrows(ApiException.class, () -> validator.validate(""));
    }
}
