package com.tibame.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 記帳工作台頁面 Page Object
 * 封裝 /ledger 頁面之自然語言快速記帳、SweetAlert2 提示捕捉、流水帳表格與財務卡片數值提取
 */
public class LedgerPage extends BasePage {

    public LedgerPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * 導航至記帳工作台
     */
    public LedgerPage navigate() {
        page.navigate(baseUrl + "/ledger");
        page.waitForSelector("#app", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    /**
     * 切換至「自然語言解析」記帳模式並輸入文字入帳
     */
    public LedgerPage submitSmartQuickInput(String text) {
        // 確保切換至自然語言解析分頁
        if (page.locator("input[placeholder*='輸入自然語言記帳']").count() == 0) {
            page.click("button:has-text('[自然語言解析]')");
        }
        page.waitForSelector("input[placeholder*='輸入自然語言記帳']");
        page.fill("input[placeholder*='輸入自然語言記帳']", text);
        page.click("button:has-text('智能解析入帳')");
        return this;
    }

    /**
     * 等待 SweetAlert2 成功彈窗或 Toast 出現
     */
    public LedgerPage assertSweetAlertSuccess(String expectedText) {
        waitForSweetAlert(5000);
        String text = page.textContent(".swal2-container");
        assertThat(text).contains(expectedText);
        return this;
    }

    /**
     * 斷言流水帳表格包含特定備註說明之記錄
     */
    public LedgerPage assertRecordExists(String description) {
        page.waitForSelector("table.table-swiss tbody tr:has-text('" + description + "')",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        return this;
    }

    /**
     * 提取頂部統計卡片之當月總支出文字
     */
    public String getTotalExpenseText() {
        page.waitForSelector(".swiss-stat-expense .swiss-stat-value");
        return page.textContent(".swiss-stat-expense .swiss-stat-value").trim();
    }

    /**
     * 提取頂部統計卡片之當月總收入文字
     */
    public String getTotalIncomeText() {
        page.waitForSelector(".swiss-stat-income .swiss-stat-value");
        return page.textContent(".swiss-stat-income .swiss-stat-value").trim();
    }

    /**
     * 提取頂部統計卡片之當月淨結餘文字
     */
    public String getNetBalanceText() {
        page.waitForSelector(".swiss-stat-balance .swiss-stat-value");
        return page.textContent(".swiss-stat-balance .swiss-stat-value").trim();
    }

    /**
     * 點擊登出按鈕並等待重導向至登入頁面
     */
    public LoginPage logout() {
        page.click("button:has-text('登出')");
        page.waitForURL(baseUrl + "/login");
        return new LoginPage(page, baseUrl);
    }
}
