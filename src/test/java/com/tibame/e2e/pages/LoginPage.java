package com.tibame.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登入與註冊頁面 Page Object
 * 封裝 /login 頁面表單操作、Tab 切換與錯誤提示斷言
 */
public class LoginPage extends BasePage {

    public LoginPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * 導航至登入頁面
     */
    public LoginPage navigate() {
        page.navigate(baseUrl + "/login");
        page.waitForSelector("#app", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    /**
     * 切換至「帳號登入」模式
     */
    public LoginPage switchToLoginTab() {
        page.click("button:has-text('帳號登入')");
        page.waitForSelector("input[autocomplete='username']");
        return this;
    }

    /**
     * 切換至「新用戶註冊」模式
     */
    public LoginPage switchToRegisterTab() {
        page.click("button:has-text('新用戶註冊')");
        page.waitForSelector("input[autocomplete='email']");
        return this;
    }

    /**
     * 填寫登入表單並送出
     */
    public LoginPage login(String username, String password) {
        switchToLoginTab();
        page.fill("input[autocomplete='username']", username);
        page.fill("input[autocomplete='current-password']", password);
        page.click("button[type='submit']");
        return this;
    }

    /**
     * 填寫註冊表單並送出
     */
    public LoginPage register(String username, String email, String displayName, String password) {
        switchToRegisterTab();
        page.fill("input[placeholder*='設定 3~50 位帳號']", username);
        page.fill("input[autocomplete='email']", email);
        if (displayName != null && !displayName.isBlank()) {
            page.fill("input[placeholder*='自訂稱呼']", displayName);
        }
        page.fill("input[autocomplete='new-password']", password);
        page.click("button[type='submit']");
        return this;
    }

    /**
     * 斷言錯誤提示包含預期文字 (支援 .alert-danger 與 SweetAlert2 彈窗)
     */
    public void assertErrorMessage(String expectedMessage) {
        if (page.locator(".alert-danger").count() > 0) {
            String text = page.textContent(".alert-danger");
            assertThat(text).contains(expectedMessage);
        } else {
            waitForSweetAlert(5000);
            String text = page.textContent(".swal2-container");
            assertThat(text).contains(expectedMessage);
        }
    }
}
