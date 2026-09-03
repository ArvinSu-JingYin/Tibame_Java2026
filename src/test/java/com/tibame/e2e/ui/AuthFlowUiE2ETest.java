package com.tibame.e2e.ui;

import com.tibame.e2e.base.PlaywrightTestBase;
import com.tibame.e2e.base.TestUserFactory;
import com.tibame.e2e.pages.LedgerPage;
import com.tibame.e2e.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 認證與導航真機 UI E2E 測試
 * 透過 Playwright 啟動無頭 Chromium 驗證使用者註冊、登入、導航跳轉至 /ledger 與 LocalStorage Token 存取
 */
class AuthFlowUiE2ETest extends PlaywrightTestBase {

    @Test
    @DisplayName("驗證真機 UI 認證黃金路徑：用戶註冊、自動/手動登入、跳轉 /ledger 且 LocalStorage 存有有效 Token")
    void testAuthAndRedirectFlow() {
        LoginPage loginPage = new LoginPage(page, getBaseUrl());
        loginPage.navigate();

        TestUserFactory.TestUser testUser = TestUserFactory.createRandomUser();

        // 1. 於註冊頁面填寫資料並送出
        loginPage.register(testUser.username(), testUser.email(), testUser.displayName(), testUser.password());

        // 2. 等待系統自動登入並導航跳轉至 /ledger
        page.waitForURL(getBaseUrl() + "/ledger");
        assertThat(page.url()).isEqualTo(getBaseUrl() + "/ledger");

        // 3. 斷言 LocalStorage 確實存放有效 JWT Token
        String token = loginPage.getJwtToken();
        assertThat(token).isNotBlank();

        // 4. 執行登出動作
        LedgerPage ledgerPage = new LedgerPage(page, getBaseUrl());
        ledgerPage.logout();

        // 5. 斷言已返回 /login 且 Token 已被清除
        assertThat(page.url()).isEqualTo(getBaseUrl() + "/login");
        assertThat(loginPage.getJwtToken()).isNull();

        // 6. 使用剛才註冊的帳號密碼於登入頁重新登入
        loginPage.login(testUser.username(), testUser.password());

        // 7. 斷言成功登入並再次跳轉至 /ledger
        page.waitForURL(getBaseUrl() + "/ledger");
        assertThat(page.url()).isEqualTo(getBaseUrl() + "/ledger");
        assertThat(loginPage.getJwtToken()).isNotBlank();
    }
}
