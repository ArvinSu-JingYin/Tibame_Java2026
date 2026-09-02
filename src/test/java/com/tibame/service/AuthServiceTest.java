package com.tibame.service;

import com.tibame.common.crypto.password.PasswordPolicyValidator;
import com.tibame.common.crypto.password.PasswordService;
import com.tibame.common.crypto.token.TokenService;
import com.tibame.common.exception.ConflictException;
import com.tibame.common.exception.UnauthorizedException;
import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RegisterRequestDto;
import com.tibame.model.entity.User;
import com.tibame.model.vo.LoginResponseVo;
import com.tibame.model.vo.UserProfileVo;
import com.tibame.repository.UserRepository;
import com.tibame.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 模組化單元測試")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);
    }

    @Test
    @DisplayName("測試正常註冊流程與密碼原則校驗")
    void testRegisterSuccess() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .username("john_doe")
                .password("SecurePass123!")
                .email("john@example.com")
                .displayName("John Doe")
                .build();

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordService.hash("SecurePass123!")).thenReturn("$2a$10$hashedPasswordValue");

        User savedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .passwordHash("$2a$10$hashedPasswordValue")
                .email("john@example.com")
                .displayName("John Doe")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserProfileVo result = authService.register(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john_doe", result.getUsername());

        verify(passwordPolicyValidator).validate("SecurePass123!");
        verify(passwordService).hash("SecurePass123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("測試登入成功且觸發無感密碼升級")
    void testLoginSuccessWithAutoUpgrade() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("john_doe")
                .password("SecurePass123!")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .username("john_doe")
                .passwordHash("$2a$04$oldLowCostHash")
                .email("john@example.com")
                .displayName("John Doe")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(existingUser));
        when(passwordService.verify("SecurePass123!", "$2a$04$oldLowCostHash")).thenReturn(true);
        when(passwordService.needsUpgrade("$2a$04$oldLowCostHash")).thenReturn(true);
        when(passwordService.hash("SecurePass123!")).thenReturn("$2a$10$newUpgradedHash");
        when(tokenService.generateToken(1L, "john_doe")).thenReturn("mocked.jwt.token");

        LoginResponseVo response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("$2a$10$newUpgradedHash", existingUser.getPasswordHash());

        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("測試錯誤密碼登入拋出 UnauthorizedException")
    void testLoginFailureOnWrongPassword() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("john_doe")
                .password("WrongPassword")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .username("john_doe")
                .passwordHash("$2a$10$correctHash")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(existingUser));
        when(passwordService.verify("WrongPassword", "$2a$10$correctHash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
        verify(tokenService, never()).generateToken(any(), any());
    }
}
