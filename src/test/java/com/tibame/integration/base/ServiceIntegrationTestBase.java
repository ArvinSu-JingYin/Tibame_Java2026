package com.tibame.integration.base;

import com.tibame.common.crypto.password.PasswordService;
import com.tibame.model.entity.User;
import com.tibame.repository.AccountRecordRepository;
import com.tibame.repository.CategoryRepository;
import com.tibame.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 服務層持久化整合測試基底抽象類別
 * <p>
 * 1. 關閉 Web 伺服器啟動 (webEnvironment = NONE)，保持極速反饋。<br>
 * 2. 類別層級宣告 @Transactional，保證所有測試方法異動 100% 自動回滾，資料零污染。<br>
 * 3. 預設套用 test 設定檔 (H2 記憶體資料庫)，可平滑切換 test-mssql 實體驗收。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@ActiveProfiles(profiles = "test", resolver = SystemPropertyActiveProfilesResolver.class)
public abstract class ServiceIntegrationTestBase {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected AccountRecordRepository accountRecordRepository;

    @Autowired
    protected PasswordService passwordService;

    @Autowired
    protected EntityManager entityManager;

    /**
     * 快速建立並持久化一個隨機測試使用者（動態 UUID 杜絕唯一鍵衝突）
     *
     * @return 已持久化之使用者實體
     */
    protected User createAndPersistTestUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .username("it_user_" + suffix)
                .email("it_user_" + suffix + "@tibame.com")
                .passwordHash(passwordService.hash("Password123!"))
                .displayName("整合測試專用用戶")
                .build();
        return userRepository.save(user);
    }
}
