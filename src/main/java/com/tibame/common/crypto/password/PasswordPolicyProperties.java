package com.tibame.common.crypto.password;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 密碼原則配置類
 */
@Data
@Component
@ConfigurationProperties(prefix = "crypto.password.policy")
public class PasswordPolicyProperties {

    /**
     * 密碼最小長度（預設 6）
     */
    private int minLength = 6;

    /**
     * 密碼最大長度（預設 64）
     */
    private int maxLength = 64;

    /**
     * 是否必須包含數字（預設 false）
     */
    private boolean requireDigit = false;

    /**
     * 是否必須包含特殊字元（預設 false）
     */
    private boolean requireSpecialChar = false;

    /**
     * 是否必須包含大寫字母（預設 false）
     */
    private boolean requireUppercase = false;

    /**
     * 是否必須包含小寫字母（預設 false）
     */
    private boolean requireLowercase = false;
}
