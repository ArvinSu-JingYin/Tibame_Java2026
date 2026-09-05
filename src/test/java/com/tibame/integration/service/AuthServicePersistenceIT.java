package com.tibame.integration.service;

import com.tibame.common.exception.ConflictException;
import com.tibame.integration.base.ServiceIntegrationTestBase;
import com.tibame.model.dto.RegisterRequestDto;
import com.tibame.model.entity.User;
import com.tibame.model.vo.UserProfileVo;
import com.tibame.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 認證服務與使用者資料落盤持久化整合測試
 * 驗證註冊成功 BCrypt 雜湊落地規格、帳號唯一性邊界與電子郵件衝突防護
 */
@DisplayName("認證服務業務持久化整合測試 (AuthServicePersistenceIT)")
class AuthServicePersistenceIT extends ServiceIntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("TC-AUTH-IT-01: 註冊成功與 BCrypt 雜湊 ($2a$10$) 落盤安全性驗證")
    void testRegisterSuccessAndBCryptHashPersistence() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String rawPassword = "StrongP@ssw0rd!";

        RegisterRequestDto registerDto = RegisterRequestDto.builder()
                .username("auth_it_" + suffix)
                .password(rawPassword)
                .email("auth_it_" + suffix + "@tibame.com")
                .displayName("認證測試專用用戶")
                .build();

        UserProfileVo profile = authService.register(registerDto);

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNotNull().isPositive();
        assertThat(profile.getUsername()).isEqualTo("auth_it_" + suffix);
        assertThat(profile.getEmail()).isEqualTo("auth_it_" + suffix + "@tibame.com");

        // 實體資料庫直查：驗證密碼絕無明文落盤且雜湊為標準 $2a$10$
        User persistedUser = userRepository.findById(profile.getId()).orElseThrow();
        assertThat(persistedUser.getPasswordHash())
                .as("密碼雜湊必須以 $2a$10$ 開頭")
                .startsWith("$2a$10$")
                .as("明文密碼絕不可直接寫入資料庫")
                .isNotEqualTo(rawPassword);

        // 驗證密碼雜湊可被正確校驗
        assertThat(passwordService.verify(rawPassword, persistedUser.getPasswordHash()))
                .as("落盤之密碼雜湊必須可成功比對原始明文密碼")
                .isTrue();
    }

    @Test
    @DisplayName("TC-AUTH-IT-02: 重複使用者帳號註冊衝突防禦 (唯一約束保護)")
    void testDuplicateUsernameRegistrationConflictDefense() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String duplicateUsername = "dup_user_" + suffix;

        // 第一次註冊成功
        RegisterRequestDto firstRegister = RegisterRequestDto.builder()
                .username(duplicateUsername)
                .password("Password123!")
                .email("dup_first_" + suffix + "@tibame.com")
                .displayName("首度註冊用戶")
                .build();
        UserProfileVo firstProfile = authService.register(firstRegister);
        assertThat(firstProfile.getId()).isNotNull();

        // 第二次嘗試以相同帳號名稱註冊（不同 Email）-> 預期拋出 ConflictException
        RegisterRequestDto secondRegister = RegisterRequestDto.builder()
                .username(duplicateUsername)
                .password("DifferentPass123!")
                .email("dup_second_" + suffix + "@tibame.com")
                .displayName("惡意重複註冊用戶")
                .build();

        assertThatThrownBy(() -> authService.register(secondRegister))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("該帳號名稱已存在");

        // 驗證首度註冊之使用者資料未受任何干擾
        User originalUser = userRepository.findById(firstProfile.getId()).orElseThrow();
        assertThat(originalUser.getUsername()).isEqualTo(duplicateUsername);
        assertThat(originalUser.getEmail()).isEqualTo("dup_first_" + suffix + "@tibame.com");
    }

    @Test
    @DisplayName("TC-AUTH-IT-03: 重複 Email 註冊衝突防禦 (信箱唯一性防護)")
    void testDuplicateEmailRegistrationConflictDefense() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String sharedEmail = "shared_email_" + suffix + "@tibame.com";

        // 第一次註冊成功
        RegisterRequestDto firstRegister = RegisterRequestDto.builder()
                .username("user_one_" + suffix)
                .password("Password123!")
                .email(sharedEmail)
                .displayName("用戶一")
                .build();
        UserProfileVo firstProfile = authService.register(firstRegister);
        assertThat(firstProfile.getId()).isNotNull();

        // 第二次嘗試以相同 Email 註冊（不同帳號名稱）-> 預期拋出 ConflictException
        RegisterRequestDto secondRegister = RegisterRequestDto.builder()
                .username("user_two_" + suffix)
                .password("Password123!")
                .email(sharedEmail)
                .displayName("用戶二")
                .build();

        assertThatThrownBy(() -> authService.register(secondRegister))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("該電子郵件已被註冊");

        // 驗證第二個帳號未被寫入資料庫
        assertThat(userRepository.findByUsername("user_two_" + suffix)).isEmpty();
    }
}
