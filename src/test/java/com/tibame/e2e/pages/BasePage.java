package com.tibame.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Page Object Model 基礎頁面類別
 * 封裝共用之 URL 導航、localStorage Token 讀取與 SweetAlert2 彈窗偵測
 */
public abstract class BasePage {

    protected final Page page;
    protected final String baseUrl;

    public BasePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public Page getPage() {
        return page;
    }

    public String getCurrentUrl() {
        return page.url();
    }

    /**
     * 從瀏覽器 LocalStorage 中讀取 JWT Token
     */
    public String getJwtToken() {
        Object token = page.evaluate("() => localStorage.getItem('jwt_token')");
        return token != null ? token.toString() : null;
    }

    /**
     * 等待 SweetAlert2 彈窗或 Toast 訊息出現
     *
     * @param timeoutMs 最大等待毫秒數
     */
    public void waitForSweetAlert(double timeoutMs) {
        page.waitForSelector(".swal2-container, .swal2-popup, .swal2-toast", new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs));
    }

    /**
     * 等待 SweetAlert2 彈窗或 Toast 訊息消失
     *
     * @param timeoutMs 最大等待毫秒數
     */
    public void waitForSweetAlertToDisappear(double timeoutMs) {
        page.waitForSelector(".swal2-container, .swal2-popup, .swal2-toast", new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeoutMs));
    }

    /**
     * 檢查 SweetAlert2 是否包含預期文字
     */
    public boolean containsSweetAlertText(String expectedText) {
        waitForSweetAlert(5000);
        String text = page.textContent(".swal2-container");
        return text != null && text.contains(expectedText);
    }
}
