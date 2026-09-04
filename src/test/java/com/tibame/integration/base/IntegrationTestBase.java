package com.tibame.integration.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 整合測試共用基底抽象類別
 * <p>
 * 提供開箱即用的 MockMvc 測試環境，並在類別層級宣告 {@link Transactional}，
 * 確保測試案例執行完畢後交易自動回滾 (Rollback)，防止測試資料污染目標資料庫。
 * </p>
 * <p>
 * 預設執行時使用 H2 記憶體資料庫；透過 {@code -Dspring.profiles.active=test-mssql}
 * 可平滑切換至本機 SQL Server (tibame_account_test) 實體測試庫。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;
}
