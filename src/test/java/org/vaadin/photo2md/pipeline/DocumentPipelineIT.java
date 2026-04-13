package org.vaadin.photo2md.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the full document pipeline with real fixture images.
 * Uses the actual implementations (OpenCV preprocessing, segmentation,
 * Tesseract OCR, Markdown generator).
 */
@SpringBootTest
class DocumentPipelineIT {

    @Autowired
    private DocumentPipeline pipeline;

    @ParameterizedTest
    @ValueSource(strings = {
            "titles-list",
            "code-blocks-1",
            "title-links-table",
            "img-titles-paragraphs-link-code"
    })
    void processesFixtureAndProducesNonEmptyMarkdown(String fixture) throws IOException {
        byte[] image = loadFixture(fixture + ".png");

        ProcessingResult result = pipeline.process(image);

        assertThat(result.markdown())
                .as("Markdown for fixture '%s'", fixture)
                .isNotBlank();
        assertThat(result.layout().elements())
                .as("Detected regions for fixture '%s'", fixture)
                .isNotEmpty();
        assertThat(result.processingTime()).isNotNull();
    }

    @Test
    void titlesListContainsRecognizedText() throws IOException {
        byte[] image = loadFixture("titles-list.png");

        ProcessingResult result = pipeline.process(image);
        String markdown = result.markdown().toLowerCase();

        // The fixture contains text about photo2md, architecture, vaadin
        // OCR may not be perfect — just verify substantial text was extracted
        assertThat(markdown).containsAnyOf("photo2m", "photo 2m");
        // Check that at least some paragraph text was recognized
        assertThat(markdown.length()).isGreaterThan(50);
    }

    @Test
    void codeBlocksContainsRecognizedText() throws IOException {
        byte[] image = loadFixture("code-blocks-1.png");

        ProcessingResult result = pipeline.process(image);
        String markdown = result.markdown().toLowerCase();

        // The fixture contains Hilla documentation with code samples
        // At minimum, verify substantial text was extracted
        assertThat(markdown.length()).isGreaterThan(50);
    }

    @Test
    void resultMetadataIsPopulated() throws IOException {
        byte[] image = loadFixture("titles-list.png");

        ProcessingResult result = pipeline.process(image);

        assertThat(result.metadata()).containsKey("regions");
        assertThat(result.metadata()).containsKey("processingTimeMs");
        int regions = Integer.parseInt(result.metadata().get("regions"));
        assertThat(regions).isGreaterThan(0);
    }

    private byte[] loadFixture(String filename) throws IOException {
        String path = "/fixtures/easy/" + filename;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertThat(is)
                    .as("Fixture file '%s' must exist on classpath", path)
                    .isNotNull();
            return is.readAllBytes();
        }
    }
}
