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

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright E2E tests for the UploadView.
 * <p>
 * Tests the full user flow: page load, upload, processing, editor, preview, download.
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

    private void navigateToApp() {
        page.navigate("http://localhost:" + port + "/");
        page.waitForSelector("#heading");
    }

    @Test
    void pageLoadsWithHeading() {
        navigateToApp();
        assertThat(page.locator("#heading")).hasText("Photo to Markdown");
    }

    @Test
    void pageLoadsWithUploadComponent() {
        navigateToApp();
        page.waitForSelector("#upload");
        assertThat(page.locator("#upload")).isVisible();
    }

    @Test
    void uploadComponentAcceptsImages() {
        navigateToApp();
        page.waitForSelector("vaadin-upload");
        assertThat(page.locator("vaadin-upload")).isVisible();
    }

    @Test
    void editorAndPreviewHiddenBeforeUpload() {
        navigateToApp();
        // SplitLayout containing editor+preview should not be visible initially
        assertThat(page.locator("#markdown-editor")).not().isVisible();
        assertThat(page.locator("#markdown-preview")).not().isVisible();
    }

    @Test
    void downloadButtonHiddenBeforeUpload() {
        navigateToApp();
        assertThat(page.locator("#download-button")).not().isVisible();
    }

    @Test
    void uploadImageShowsEditorAndPreview() {
        navigateToApp();

        // Upload a fixture image
        Path fixturePath = Path.of("src/test/resources/fixtures/easy/titles-list.png");
        page.locator("vaadin-upload input[type='file']")
                .setInputFiles(fixturePath);

        // Wait for processing to complete — editor becomes visible
        page.locator("#markdown-editor").waitFor(
                new com.microsoft.playwright.Locator.WaitForOptions()
                        .setTimeout(60000));

        assertThat(page.locator("#markdown-editor")).isVisible();
        assertThat(page.locator("#markdown-preview")).isVisible();
        assertThat(page.locator("#download-button")).isVisible();
    }

    @Test
    void uploadedImageProducesMarkdownContent() {
        navigateToApp();

        Path fixturePath = Path.of("src/test/resources/fixtures/easy/titles-list.png");
        page.locator("vaadin-upload input[type='file']")
                .setInputFiles(fixturePath);

        // Wait for editor to appear with content
        page.locator("#markdown-editor").waitFor(
                new com.microsoft.playwright.Locator.WaitForOptions()
                        .setTimeout(60000));

        // The textarea should have some markdown content
        String editorContent = page.locator("#markdown-editor textarea")
                .inputValue();
        assertThat(page.locator("#markdown-editor textarea"))
                .not().hasValue("");
    }
}
