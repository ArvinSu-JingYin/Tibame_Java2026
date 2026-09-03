package com.tibame.e2e.api;

import com.tibame.common.response.ApiResponse;
import com.tibame.e2e.base.TestUserFactory;
import com.tibame.model.dto.CategoryCreateRequestDto;
import com.tibame.model.dto.CategoryUpdateRequestDto;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.vo.CategoryResponseVo;
import com.tibame.model.vo.RecordResponseVo;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨租戶橫向越權攻擊穿透防護 API E2E 整合測試
 * 驗證用戶 B 嘗試讀取、修改或刪除用戶 A 之自訂分類與流水帳紀錄時必定被嚴格攔截 (403 Forbidden / 404 Not Found)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TenantIsolationSecurityE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
    }

    @Test
    @DisplayName("驗證多租戶資料隔離防護：用戶 B 橫向越權存取用戶 A 之分類與記帳紀錄皆被嚴格拒絕")
    void testTenantIsolationSecurity() {
        // 1. 註冊並登入用戶 A
        String tokenA = registerAndLogin();
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);

        // 2. 註冊並登入用戶 B
        String tokenB = registerAndLogin();
        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);

        // 3. 用戶 A 建立自訂私房分類
        CategoryCreateRequestDto createCatDto = CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("用戶A私房分類")
                .iconCode("lock")
                .sortOrder(90)
                .build();
        ResponseEntity<ApiResponse<CategoryResponseVo>> catResA = restTemplate.exchange(
                "/api/v1/categories",
                HttpMethod.POST,
                new HttpEntity<>(createCatDto, headersA),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(catResA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long categoryAId = catResA.getBody().getData().getId();

        // 4. 用戶 A 建立記帳紀錄
        RecordCreateRequestDto createRecordDto = RecordCreateRequestDto.builder()
                .recordType("EXPENSE")
                .categoryId(categoryAId)
                .amount(new BigDecimal("888.00"))
                .description("用戶A秘密支出")
                .recordDate(LocalDate.now())
                .build();
        ResponseEntity<ApiResponse<RecordResponseVo>> recordResA = restTemplate.exchange(
                "/api/v1/records",
                HttpMethod.POST,
                new HttpEntity<>(createRecordDto, headersA),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(recordResA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long recordAId = recordResA.getBody().getData().getId();

        // 5. 用戶 B 嘗試讀取用戶 A 的自訂分類 -> 預期 404 (因 findAvailableById 找不到該使用者所屬分類)
        ResponseEntity<ApiResponse<Void>> catGetResB = restTemplate.exchange(
                "/api/v1/categories/" + categoryAId,
                HttpMethod.GET,
                new HttpEntity<>(headersB),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(catGetResB.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);

        // 6. 用戶 B 嘗試更新用戶 A 的自訂分類 -> 預期 403 Forbidden
        CategoryUpdateRequestDto updateCatDto = CategoryUpdateRequestDto.builder()
                .name("用戶B竄改名稱")
                .build();
        ResponseEntity<ApiResponse<Void>> catUpdateResB = restTemplate.exchange(
                "/api/v1/categories/" + categoryAId,
                HttpMethod.PUT,
                new HttpEntity<>(updateCatDto, headersB),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(catUpdateResB.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 7. 用戶 B 嘗試讀取用戶 A 的記帳紀錄 -> 預期 404 Not Found
        ResponseEntity<ApiResponse<Void>> recordGetResB = restTemplate.exchange(
                "/api/v1/records/" + recordAId,
                HttpMethod.GET,
                new HttpEntity<>(headersB),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(recordGetResB.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);

        // 8. 用戶 B 嘗試刪除用戶 A 的記帳紀錄 -> 預期 404 Not Found (或 403 Forbidden)
        ResponseEntity<ApiResponse<Void>> recordDeleteResB = restTemplate.exchange(
                "/api/v1/records/" + recordAId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersB),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(recordDeleteResB.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    private String registerAndLogin() {
        TestUserFactory.TestUser user = TestUserFactory.createRandomUser();
        restTemplate.postForEntity("/api/v1/auth/register", TestUserFactory.toRegisterRequest(user), ApiResponse.class);

        ResponseEntity<ApiResponse<Map<String, Object>>> loginRes = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(TestUserFactory.toLoginRequest(user)),
                new ParameterizedTypeReference<>() {}
        );
        return (String) loginRes.getBody().getData().get("token");
    }
}
