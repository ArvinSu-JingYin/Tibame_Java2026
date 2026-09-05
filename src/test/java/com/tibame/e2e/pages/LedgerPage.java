package com.tibame.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 記帳工作台頁面 Page Object
 * 封裝 /ledger 瑞士風格四大分頁工作台之分頁切換、錄入表單、自動流轉斷言、交易明細篩選與財務卡片數值提取
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
     * 切換至指定分頁並等待 active 樣式類別穩定
     *
     * @param tabName 分頁代碼 ('entry'/'01', 'history'/'02', 'analytics'/'03', 'categories'/'04')
     */
    public LedgerPage switchTab(String tabName) {
        String keyword = getTabKeyword(tabName);
        if (page.locator(".swiss-tab-btn.active:has-text('" + keyword + "')").count() == 0) {
            page.click(".swiss-tab-btn:has-text('" + keyword + "')");
            page.waitForSelector(".swiss-tab-btn.active:has-text('" + keyword + "')");
        }
        return this;
    }

    /**
     * 切換至「自然語言解析」記帳模式並輸入文字入帳
     * 入帳後自動斷言平滑流轉至「02 交易明細」分頁
     */
    public LedgerPage submitSmartQuickInput(String text) {
        switchTab("entry");
        if (page.locator(".swiss-entry-mode-btn.active:has-text('自然語言解析')").count() == 0) {
            page.click(".swiss-entry-mode-btn:has-text('自然語言解析')");
        }
        page.waitForSelector(".swiss-amount-input[placeholder*='例如：午餐便當']");
        page.fill(".swiss-amount-input[placeholder*='例如：午餐便當']", text);
        page.click("button:has-text('智能解析入帳')");

        // 核心 UX 契約：記帳成功後平滑自動流轉至 02 交易明細分頁
        page.waitForSelector(".swiss-tab-btn.active:has-text('02')");
        return this;
    }

    /**
     * 切換至「結構化錄入」記帳模式填寫表單並入帳
     * 入帳後自動斷言平滑流轉至「02 交易明細」分頁
     *
     * @param type         收支類型 ("EXPENSE" 或 "INCOME")
     * @param amount       金額
     * @param categoryName 分類名稱 (如 "餐飲飲食")
     * @param note         備註說明
     */
    public LedgerPage submitStructuredInput(String type, double amount, String categoryName, String note) {
        switchTab("entry");
        if (page.locator(".swiss-entry-mode-btn.active:has-text('結構化錄入')").count() == 0) {
            page.click(".swiss-entry-mode-btn:has-text('結構化錄入')");
        }

        // 選擇收支類型
        String typeLabel = "EXPENSE".equalsIgnoreCase(type) || "支出".equals(type) ? "支出" : "收入";
        page.click(".type-toggle-btn:has-text('" + typeLabel + "')");

        // 填寫金額
        page.fill("#quickAmountInput", String.valueOf(amount));

        // 選擇項目分類
        Locator option = page.locator("select.form-select-swiss option:has-text('" + categoryName + "')");
        if (option.count() > 0) {
            String catId = option.first().getAttribute("value");
            page.selectOption("select.form-select-swiss", catId);
        } else {
            page.selectOption("select.form-select-swiss", new SelectOption().setLabel(categoryName));
        }

        // 填寫備註
        if (note != null && !note.isBlank()) {
            page.fill("input[placeholder*='請輸入備註說明']", note);
        }

        // 點擊送出
        page.click("button[type='submit']:has-text('入帳送出')");

        // 核心 UX 契約：記帳成功後平滑自動流轉至 02 交易明細分頁
        page.waitForSelector(".swiss-tab-btn.active:has-text('02')");
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
        switchTab("history");
        page.waitForSelector("table.table-swiss tbody tr:has-text('" + description + "')",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        return this;
    }

    /**
     * 展開/收合交易明細分頁之多維度篩選器面板
     */
    public LedgerPage toggleFilterPanel() {
        switchTab("history");
        page.click("button:has-text('多維度篩選'), button:has-text('隱藏篩選器')");
        return this;
    }

    /**
     * 使用關鍵字進行交易明細快速篩選
     */
    public LedgerPage filterByKeyword(String keyword) {
        switchTab("history");
        if (page.locator("input[placeholder*='搜尋備註']").count() == 0) {
            toggleFilterPanel();
        }
        page.fill("input[placeholder*='搜尋備註']", keyword);
        page.click("button:has-text('搜尋')");
        return this;
    }

    /**
     * 提取分頁 03 (財務概覽) 統計卡片之當月總支出文字
     * 自動確保分頁切換至可見狀態，消除 v-show 隱藏元素造成之超時
     */
    public String getTotalExpenseText() {
        switchTab("analytics");
        page.waitForSelector(".swiss-stat-expense .swiss-stat-value");
        return page.textContent(".swiss-stat-expense .swiss-stat-value").trim();
    }

    /**
     * 提取分頁 03 (財務概覽) 統計卡片之當月總收入文字
     * 自動確保分頁切換至可見狀態，消除 v-show 隱藏元素造成之超時
     */
    public String getTotalIncomeText() {
        switchTab("analytics");
        page.waitForSelector(".swiss-stat-income .swiss-stat-value");
        return page.textContent(".swiss-stat-income .swiss-stat-value").trim();
    }

    /**
     * 提取分頁 03 (財務概覽) 統計卡片之當月淨結餘文字
     * 自動確保分頁切換至可見狀態，消除 v-show 隱藏元素造成之超時
     */
    public String getNetBalanceText() {
        switchTab("analytics");
        page.waitForSelector(".swiss-stat-balance .swiss-stat-value");
        return page.textContent(".swiss-stat-balance .swiss-stat-value").trim();
    }

    /**
     * 於分頁 04 (分類管理) 新增自訂收支分類
     *
     * @param type     "EXPENSE" 或 "INCOME"
     * @param name     分類名稱
     * @param iconCode 圖標代碼 (如 "cart", "film", "tag")
     */
    public LedgerPage createCategory(String type, String name, String iconCode) {
        switchTab("categories");
        page.waitForSelector("input[placeholder*='分類名稱']");

        // 選擇類型
        Locator typeSelect = page.locator("div[v-show*='categories'] select.form-select-swiss").first();
        typeSelect.selectOption(type);

        // 輸入名稱
        page.fill("input[placeholder*='分類名稱']", name);

        // 選擇圖標
        if (iconCode != null && !iconCode.isBlank()) {
            Locator iconSelect = page.locator("div[v-show*='categories'] select:has(option[value='cart'])");
            if (iconSelect.count() > 0) {
                iconSelect.selectOption(iconCode);
            }
        }

        // 送出
        page.click("button:has-text('新增分類')");

        // 斷言表格渲染新分類
        page.waitForSelector("table.table-swiss tbody tr:has-text('" + name + "')");
        return this;
    }

    /**
     * 點擊登出按鈕並等待重導向至登入頁面
     */
    public LoginPage logout() {
        page.click("button:has-text('登出')");
        page.waitForURL(baseUrl + "/login");
        return new LoginPage(page, baseUrl);
    }

    private String getTabKeyword(String tabName) {
        if (tabName == null) return "01";
        return switch (tabName.toLowerCase().trim()) {
            case "history", "02", "ledger" -> "02";
            case "analytics", "03" -> "03";
            case "categories", "04" -> "04";
            default -> "01";
        };
    }
}
