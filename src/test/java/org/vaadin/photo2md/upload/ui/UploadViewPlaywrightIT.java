package org.vaadin.photo2md.upload.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright E2E tests for the UploadView.
 * <p>
 * Verifies the upload page loads correctly and key UI components are present.
 * See spec: src/test/resources/specs/06-ui-upload-and-preview.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("playwright")
class UploadViewPlaywrightIT {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;
    private Page page;

    @BeforeAll
    static void setupBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createPage() {
        page = browser.newPage();
    }

    @AfterEach
    void closePage() {
        if (page != null) {
            page.close();
        }
    }

    @Test
    void pageLoadsWithHeading() {
        page.navigate("http://localhost:" + port + "/");
        // Wait for Vaadin to finish loading
        page.waitForSelector("#heading");

        assertThat(page.locator("#heading")).hasText("Photo to Markdown");
    }

    @Test
    void pageLoadsWithUploadComponent() {
        page.navigate("http://localhost:" + port + "/");
        page.waitForSelector("#upload");

        assertThat(page.locator("#upload")).isVisible();
    }

    @Test
    void uploadComponentAcceptsImages() {
        page.navigate("http://localhost:" + port + "/");
        page.waitForSelector("vaadin-upload");

        // Vaadin Upload renders as a vaadin-upload web component
        assertThat(page.locator("vaadin-upload")).isVisible();
    }
}
