package com.tibame.integration.base;

import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.util.StringUtils;

/**
 * 支援系統屬性動態切換之 ActiveProfilesResolver
 * <p>
 * 預設載入 "test" (H2 記憶體資料庫)；<br>
 * 當命令列傳入 {@code -Dspring.profiles.active=test-mssql} 時，優先採用命令列指定之 Profile。
 * </p>
 */
public class SystemPropertyActiveProfilesResolver implements ActiveProfilesResolver {

    @Override
    @NonNull
    public String[] resolve(@NonNull Class<?> testClass) {
        String activeProfile = System.getProperty("spring.profiles.active");
        if (StringUtils.hasText(activeProfile)) {
            return new String[]{activeProfile.trim()};
        }
        return new String[]{"test"};
    }
}
