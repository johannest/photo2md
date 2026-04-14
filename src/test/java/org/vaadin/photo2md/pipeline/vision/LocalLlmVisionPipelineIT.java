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
 * Integration tests for {@link LocalLlmVisionPipeline} with a real local LLM.
 * Requires a running OpenAI-compatible server (e.g., LM Studio on localhost:1234).
 * <p>
 * Run with: {@code mvn verify -Dgroups=local-llm -Dspring.profiles.active=local-llm}
 */
@SpringBootTest
@ActiveProfiles("local-llm")
@Tag("local-llm")
class LocalLlmVisionPipelineIT {

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
        assertThat(result.metadata()).containsEntry("backend", "local-llm");
    }

    @Test
    void titlesListContainsExpectedContent() throws IOException {
        byte[] image = loadFixture("titles-list.png");

        ProcessingResult result = pipeline.process(image);
        String markdown = result.markdown().toLowerCase();

        // Local LLMs may have lower accuracy, so we check for at least some content
        assertThat(markdown.length()).isGreaterThan(50);
        assertThat(markdown).containsAnyOf("photo2md", "photo", "architecture", "vaadin");
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
