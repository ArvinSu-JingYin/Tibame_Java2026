package com.tibame.integration;

import com.tibame.integration.base.IntegrationTestBase;
import com.tibame.model.entity.User;
import com.tibame.repository.CategoryRepository;
import com.tibame.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真機資料庫與交易回滾整合測試
 * 繼承 {@link IntegrationTestBase}，支援本機 SQL Server (tibame_account_test) 與 H2 雙模式驗證
 */
@DisplayName("真機資料庫連線與交易自動回滾整合測試")
class DatabaseIntegrationIT extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("驗證系統種子資料自動初始化與載入成功")
    void testSeedDataInitialization() {
        long categoryCount = categoryRepository.count();
        assertThat(categoryCount).as("系統初始化應至少具備 11 筆種子分類").isGreaterThanOrEqualTo(11L);
    }

    @Test
    @DisplayName("驗證資料寫入、動態主鍵指派 (防範 IDENTITY 跳號) 與交易回滾機制")
    void testEntityPersistenceAndDynamicIdAssertion() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        User testUser = User.builder()
                .username("mssql_test_" + uniqueSuffix)
                .passwordHash("$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW")
                .email("mssql_test_" + uniqueSuffix + "@tibame.com")
                .displayName("MSSQL 測試用戶")
                .build();

        User savedUser = userRepository.save(testUser);

        // 防禦標準：動態主鍵斷言，嚴禁硬編碼 ID (如 assertEquals(1L, id))
        assertThat(savedUser.getId())
                .as("實體保存後主鍵 ID 必須非空且為正數")
                .isNotNull()
                .isPositive();

        Optional<User> found = userRepository.findById(savedUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(testUser.getUsername());
    }
}
