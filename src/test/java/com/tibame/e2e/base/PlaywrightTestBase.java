package com.tibame.e2e.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Playwright 真機 UI E2E 測試基底類別
 * 管理 Spring Boot 隨機埠容器啟動、Playwright Chromium 瀏覽器實例
 * 以及每個測試案例獨立之 BrowserContext 無痕沙盒生命週期
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class PlaywrightTestBase {

    @LocalServerPort
    protected int port;

    protected static Playwright playwright;
    protected static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void initPlaywright() {
        if (playwright == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
        }
    }

    @AfterAll
    static void destroyPlaywright() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    @BeforeEach
    void initBrowserContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void destroyBrowserContext() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * 取得目前測試服務之基礎 URL
     *
     * @return 例如 "http://localhost:54321"
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
