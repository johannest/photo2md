package org.vaadin.photo2md.pipeline.vision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClaudeVisionPipeline} with mocked RestClient.
 */
@ExtendWith(MockitoExtension.class)
class ClaudeVisionPipelineTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ClaudeVisionPipeline pipeline;

    private static final byte[] TEST_IMAGE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @BeforeEach
    void setUp() {
        pipeline = new ClaudeVisionPipeline(restClient, "claude-sonnet-4-20250514", 4096);
    }

    private void stubRestClient(Map<String, Object> response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(String.class), any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);
    }

    private Map<String, Object> successResponse(String markdown) {
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", markdown)),
                "model", "claude-sonnet-4-20250514",
                "role", "assistant"
        );
    }

    @Nested
    class RequestFormatting {

        @Test
        void sendsAnthropicVersionHeader() {
            stubRestClient(successResponse("# Hello"));

            pipeline.process(TEST_IMAGE);

            verify(requestBodySpec).header("anthropic-version", "2023-06-01");
        }

        @Test
        @SuppressWarnings("unchecked")
        void sendsBase64ImageInRequestBody() {
            stubRestClient(successResponse("# Hello"));

            pipeline.process(TEST_IMAGE);

            ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(requestBodySpec).body(bodyCaptor.capture());

            Map<String, Object> body = bodyCaptor.getValue();
            assertThat(body).containsKey("model");
            assertThat(body).containsKey("max_tokens");
            assertThat(body).containsKey("messages");

            var messages = (List<Map<String, Object>>) body.get("messages");
            assertThat(messages).hasSize(1);

            var content = (List<Map<String, Object>>) messages.getFirst().get("content");
            assertThat(content).hasSize(2);

            // First block: image
            assertThat(content.get(0)).containsEntry("type", "image");
            var source = (Map<String, Object>) content.get(0).get("source");
            assertThat(source).containsEntry("type", "base64");
            assertThat(source).containsEntry("media_type", "image/png");
            assertThat((String) source.get("data")).isNotBlank();

            // Second block: text prompt
            assertThat(content.get(1)).containsEntry("type", "text");
            assertThat((String) content.get(1).get("text")).contains("document-to-Markdown");
        }

        @Test
        void sendsConfiguredModel() {
            var customPipeline = new ClaudeVisionPipeline(restClient, "claude-opus-4-20250514", 8192);
            stubRestClient(successResponse("# Hello"));

            customPipeline.process(TEST_IMAGE);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(requestBodySpec).body(bodyCaptor.capture());

            assertThat(bodyCaptor.getValue()).containsEntry("model", "claude-opus-4-20250514");
            assertThat(bodyCaptor.getValue()).containsEntry("max_tokens", 8192);
        }
    }

    @Nested
    class ResponseParsing {

        @Test
        void extractsMarkdownFromResponse() {
            stubRestClient(successResponse("# Document Title\n\nSome text."));

            ProcessingResult result = pipeline.process(TEST_IMAGE);

            assertThat(result.markdown()).isEqualTo("# Document Title\n\nSome text.");
        }

        @Test
        void throwsOnNullResponse() {
            stubRestClient(null);

            assertThatThrownBy(() -> pipeline.process(TEST_IMAGE))
                    .isInstanceOf(VisionApiException.class)
                    .hasMessageContaining("null response");
        }

        @Test
        void throwsOnEmptyContent() {
            stubRestClient(Map.of("content", List.of()));

            assertThatThrownBy(() -> pipeline.process(TEST_IMAGE))
                    .isInstanceOf(VisionApiException.class)
                    .hasMessageContaining("empty content");
        }

        @Test
        void throwsOnBlankText() {
            stubRestClient(Map.of(
                    "content", List.of(Map.of("type", "text", "text", "  "))
            ));

            assertThatThrownBy(() -> pipeline.process(TEST_IMAGE))
                    .isInstanceOf(VisionApiException.class)
                    .hasMessageContaining("empty text");
        }
    }

    @Nested
    class Metadata {

        @Test
        void backendNameIsClaude() {
            stubRestClient(successResponse("# Hello"));

            ProcessingResult result = pipeline.process(TEST_IMAGE);

            assertThat(result.metadata()).containsEntry("backend", "claude");
        }
    }
}
