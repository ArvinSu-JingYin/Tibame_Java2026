package com.tibame.common.crypto.password;

/**
 * 密碼原則與強度驗證介面
 */
public interface PasswordPolicyValidator {

    /**
     * 驗證明文密碼是否符合配置的密碼複雜度原則
     *
     * @param rawPassword 明文密碼
     * @throws com.tibame.common.exception.ApiException 若不符合規則拋出驗證異常
     */
    void validate(String rawPassword);

    /**
     * 檢查密碼是否符合規則
     *
     * @param rawPassword 明文密碼
     * @return 符合回傳 true，否則回傳 false
     */
    boolean isValid(String rawPassword);
}
