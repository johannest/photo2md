package org.vaadin.photo2md.pipeline.vision;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vaadin.photo2md.pipeline.DocumentPipeline;
import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClaudeVisionPipeline} with real API calls.
 * Requires {@code ANTHROPIC_API_KEY} environment variable.
 * <p>
 * Run with: {@code mvn verify -Dgroups=claude -Dspring.profiles.active=claude}
 */
@SpringBootTest
@ActiveProfiles("claude")
@Tag("claude")
class ClaudeVisionPipelineIT {

    @Autowired
    private DocumentPipeline pipeline;

    @ParameterizedTest
    @ValueSource(strings = {
            "titles-list",
            "code-blocks-1",
            "title-links-table",
            "img-titles-paragraphs-link-code"
    })
    void processesFixtureAndProducesMarkdown(String fixture) throws IOException {
        byte[] image = loadFixture(fixture + ".png");

        ProcessingResult result = pipeline.process(image);

        assertThat(result.markdown())
                .as("Markdown for fixture '%s'", fixture)
                .isNotBlank();
        assertThat(result.metadata()).containsEntry("backend", "claude");
    }

    @Test
    void titlesListContainsExpectedContent() throws IOException {
        byte[] image = loadFixture("titles-list.png");

        ProcessingResult result = pipeline.process(image);
        String markdown = result.markdown().toLowerCase();

        assertThat(markdown).contains("photo2md");
        assertThat(markdown).contains("architecture");
        assertThat(markdown).containsAnyOf("vaadin", "spring");
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
