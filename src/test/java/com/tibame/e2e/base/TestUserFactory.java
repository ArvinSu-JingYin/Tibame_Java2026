package com.tibame.e2e.base;

import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RegisterRequestDto;

import java.util.UUID;

/**
 * E2E 測試動態用戶工廠
 * 提供動態生成帶隨機 UUID 尾碼之測試用戶資料，防止併發與重複執行時之 409 Conflict 衝突
 */
public final class TestUserFactory {

    private TestUserFactory() {
        // 工具類禁止實例化
    }

    public record TestUser(
            String username,
            String password,
            String email,
            String displayName
    ) {}

    /**
     * 生成帶隨機 UUID 尾碼的測試使用者資料
     *
     * @return 具備唯一使用者名稱與合規密碼的 TestUser
     */
    public static TestUser createRandomUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "test_user_" + suffix;
        String password = "TestPass123!#" + suffix.substring(0, 4);
        String email = "test_" + suffix + "@example.com";
        String displayName = "Test User " + suffix;
        return new TestUser(username, password, email, displayName);
    }

    /**
     * 轉為註冊請求 DTO
     */
    public static RegisterRequestDto toRegisterRequest(TestUser user) {
        return RegisterRequestDto.builder()
                .username(user.username())
                .password(user.password())
                .email(user.email())
                .displayName(user.displayName())
                .build();
    }

    /**
     * 轉為登入請求 DTO
     */
    public static LoginRequestDto toLoginRequest(TestUser user) {
        return LoginRequestDto.builder()
                .username(user.username())
                .password(user.password())
                .build();
    }
}
