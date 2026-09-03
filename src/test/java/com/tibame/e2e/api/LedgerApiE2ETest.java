package com.tibame.e2e.api;

import com.tibame.common.response.ApiResponse;
import com.tibame.e2e.base.TestUserFactory;
import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.dto.RegisterRequestDto;
import com.tibame.model.vo.MonthlySummaryVo;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流水帳業務全鏈路 API E2E 整合測試
 * 驗證分類查詢、新增記帳、自然語言快速記帳、月度統計計算以及刪除流水帳之完整業務閉環
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LedgerApiE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
    }

    @Test
    @DisplayName("驗證記帳業務閉環：分類查詢、記帳新增、自然語言解析、月度統計計算與刪除")
    void testLedgerFullLifecycle() {
        // 1. 建立測試使用者並獲取 Token
        TestUserFactory.TestUser testUser = TestUserFactory.createRandomUser();
        RegisterRequestDto registerDto = TestUserFactory.toRegisterRequest(testUser);
        restTemplate.postForEntity("/api/v1/auth/register", registerDto, ApiResponse.class);

        LoginRequestDto loginDto = TestUserFactory.toLoginRequest(testUser);
        ResponseEntity<ApiResponse<Map<String, Object>>> loginRes = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginDto),
                new ParameterizedTypeReference<>() {}
        );
        String token = (String) loginRes.getBody().getData().get("token");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // 2. 查詢可用分類 (確保系統預設分類已載入)
        ResponseEntity<ApiResponse<List<Map<String, Object>>>> catRes = restTemplate.exchange(
                "/api/v1/categories?type=EXPENSE",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(catRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> categories = catRes.getBody().getData();
        assertThat(categories).isNotEmpty();
        Long expenseCategoryId = ((Number) categories.get(0).get("id")).longValue();

        // 3. 建立常規支出記帳
        LocalDate today = LocalDate.now();
        RecordCreateRequestDto createDto = RecordCreateRequestDto.builder()
                .recordType("EXPENSE")
                .categoryId(expenseCategoryId)
                .amount(new BigDecimal("150.00"))
                .description("晚餐牛排")
                .recordDate(today)
                .build();

        ResponseEntity<ApiResponse<RecordResponseVo>> createRes = restTemplate.exchange(
                "/api/v1/records",
                HttpMethod.POST,
                new HttpEntity<>(createDto, headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RecordResponseVo createdRecord = createRes.getBody().getData();
        assertThat(createdRecord.getId()).isNotNull();
        assertThat(createdRecord.getAmount()).isEqualByComparingTo("150.00");

        // 4. 自然語言快速記帳
        Map<String, String> quickPayload = Map.of("text", "午餐便當 120");
        ResponseEntity<ApiResponse<RecordResponseVo>> quickRes = restTemplate.exchange(
                "/api/v1/records/quick",
                HttpMethod.POST,
                new HttpEntity<>(quickPayload, headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(quickRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RecordResponseVo quickRecord = quickRes.getBody().getData();
        assertThat(quickRecord.getId()).isNotNull();
        assertThat(quickRecord.getAmount()).isEqualByComparingTo("120.00");

        // 5. 查詢月度統計 (150 + 120 = 270)
        ResponseEntity<ApiResponse<MonthlySummaryVo>> summaryRes = restTemplate.exchange(
                "/api/v1/records/summary?year=" + today.getYear() + "&month=" + today.getMonthValue(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(summaryRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        MonthlySummaryVo summary = summaryRes.getBody().getData();
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("270.00");

        // 6. 刪除第一筆記帳
        ResponseEntity<ApiResponse<Void>> deleteRes = restTemplate.exchange(
                "/api/v1/records/" + createdRecord.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. 再次查詢月度統計，確認只剩 120
        ResponseEntity<ApiResponse<MonthlySummaryVo>> afterDeleteSummaryRes = restTemplate.exchange(
                "/api/v1/records/summary?year=" + today.getYear() + "&month=" + today.getMonthValue(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        MonthlySummaryVo afterSummary = afterDeleteSummaryRes.getBody().getData();
        assertThat(afterSummary.getTotalExpense()).isEqualByComparingTo("120.00");
    }
}
