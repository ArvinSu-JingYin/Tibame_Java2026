package com.tibame.e2e.api;

import com.tibame.common.response.ApiResponse;
import com.tibame.e2e.base.TestUserFactory;
import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RegisterRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 認證與授權全鏈路 API E2E 整合測試
 * 驗證使用者註冊、衝突拒絕 (409)、錯誤密碼拒絕 (401)、登入取得 Token 與 /auth/me 認證
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthApiE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
    }

    @Test
    @DisplayName("驗證完整認證週期：註冊、重複名稱衝突(409)、密碼錯誤(401)及登入成功簽發 Bearer Token")
    void testAuthFullLifecycle() {
        TestUserFactory.TestUser testUser = TestUserFactory.createRandomUser();
        RegisterRequestDto registerDto = TestUserFactory.toRegisterRequest(testUser);

        // 1. 成功註冊新用戶
        ResponseEntity<ApiResponse<Map<String, Object>>> registerResponse = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerDto),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().isSuccess()).isTrue();

        // 2. 重複註冊同名帳號 -> 預期 409 Conflict
        ResponseEntity<ApiResponse<Void>> duplicateResponse = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerDto),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 3. 錯誤密碼登入 -> 預期 401 Unauthorized
        LoginRequestDto wrongPasswordDto = LoginRequestDto.builder()
                .username(testUser.username())
                .password("WrongPassword999!#")
                .build();
        ResponseEntity<ApiResponse<Void>> unauthorizedResponse = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(wrongPasswordDto),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(unauthorizedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 4. 正確帳號密碼登入 -> 成功取得 Bearer Token
        LoginRequestDto loginDto = TestUserFactory.toLoginRequest(testUser);
        ResponseEntity<ApiResponse<Map<String, Object>>> loginResponse = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginDto),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().isSuccess()).isTrue();

        Map<String, Object> data = loginResponse.getBody().getData();
        assertThat(data).containsKey("token");
        String token = (String) data.get("token");
        assertThat(token).isNotBlank();

        // 5. 攜帶 Bearer Token 存取 /auth/me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ApiResponse<Map<String, Object>>> meResponse = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().getData().get("username")).isEqualTo(testUser.username());
    }
}
