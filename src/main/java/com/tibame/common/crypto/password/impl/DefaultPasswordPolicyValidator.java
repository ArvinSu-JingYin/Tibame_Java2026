package com.tibame.common.crypto.password.impl;

import com.tibame.common.crypto.password.PasswordPolicyProperties;
import com.tibame.common.crypto.password.PasswordPolicyValidator;
import com.tibame.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 預設密碼原則檢驗器實作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultPasswordPolicyValidator implements PasswordPolicyValidator {

    private final PasswordPolicyProperties properties;

    @Override
    public void validate(String rawPassword) {
        List<String> errors = checkErrors(rawPassword);
        if (!errors.isEmpty()) {
            String message = "密碼不符合安全原則: " + String.join(", ", errors);
            log.warn("密碼複雜度校驗失敗: {}", message);
            throw new ApiException(400, message);
        }
    }

    @Override
    public boolean isValid(String rawPassword) {
        return checkErrors(rawPassword).isEmpty();
    }

    private List<String> checkErrors(String rawPassword) {
        List<String> errors = new ArrayList<>();
        if (rawPassword == null || rawPassword.isEmpty()) {
            errors.add("密碼不得為空");
            return errors;
        }

        if (rawPassword.length() < properties.getMinLength()) {
            errors.add("長度至少需 " + properties.getMinLength() + " 個字元");
        }
        if (rawPassword.length() > properties.getMaxLength()) {
            errors.add("長度不得超過 " + properties.getMaxLength() + " 個字元");
        }
        if (properties.isRequireDigit() && !rawPassword.matches(".*\\d.*")) {
            errors.add("必須包含至少一個數字");
        }
        if (properties.isRequireUppercase() && !rawPassword.matches(".*[A-Z].*")) {
            errors.add("必須包含至少一個大寫英文字母");
        }
        if (properties.isRequireLowercase() && !rawPassword.matches(".*[a-z].*")) {
            errors.add("必須包含至少一個小寫英文字母");
        }
        if (properties.isRequireSpecialChar() && !rawPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            errors.add("必須包含至少一個特殊符號");
        }

        return errors;
    }
}
