package com.tibame.e2e.ui;

import com.tibame.e2e.base.PlaywrightTestBase;
import com.tibame.e2e.base.TestUserFactory;
import com.tibame.e2e.pages.LedgerPage;
import com.tibame.e2e.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 核心記帳業務黃金路徑真機 UI E2E 測試
 * 透過 Playwright 操作智慧記帳自然語言輸入、自動捕捉 SweetAlert2 彈窗、斷言表格即時刷新與財務卡片動態統計
 */
class AccountingFlowUiE2ETest extends PlaywrightTestBase {

    @Test
    @DisplayName("驗證記帳黃金路徑：智慧自然語言記帳、SweetAlert2 提示、流水帳表格即時渲染、統計卡片更新與安全登出")
    void testAccountingFlow() {
        // 1. 登入系統並導航至記帳工作台
        LoginPage loginPage = new LoginPage(page, getBaseUrl());
        loginPage.navigate();

        TestUserFactory.TestUser testUser = TestUserFactory.createRandomUser();
        loginPage.register(testUser.username(), testUser.email(), testUser.displayName(), testUser.password());

        page.waitForURL(getBaseUrl() + "/ledger");
        LedgerPage ledgerPage = new LedgerPage(page, getBaseUrl());

        // 2. 斷言初始當月總支出為 0.00
        assertThat(ledgerPage.getTotalExpenseText()).contains("0.00");

        // 3. 操作智慧自然語言記帳，輸入 "午餐 120"
        ledgerPage.submitSmartQuickInput("午餐 120 飲食聚餐");

        // 4. 等待並斷言 SweetAlert2 成功彈窗
        ledgerPage.assertSweetAlertSuccess("智能解析入帳成功");

        // 5. 斷言下方流水帳表格即時出現 "午餐"
        ledgerPage.assertRecordExists("午餐");

        // 6. 斷言上方統計卡片當月總支出動態更新為 120.00
        assertThat(ledgerPage.getTotalExpenseText()).contains("120.00");

        // 7. 點擊登出
        LoginPage afterLogoutPage = ledgerPage.logout();

        // 8. 斷言重導向至 /login 且 Token 已被清除
        assertThat(page.url()).isEqualTo(getBaseUrl() + "/login");
        assertThat(afterLogoutPage.getJwtToken()).isNull();
    }
}
