package org.vaadin.photo2md.pipeline.vision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * Unit tests for {@link LocalLlmVisionPipeline} with mocked RestClient.
 */
@ExtendWith(MockitoExtension.class)
class LocalLlmVisionPipelineTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private LocalLlmVisionPipeline pipeline;

    private static final byte[] TEST_IMAGE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @BeforeEach
    void setUp() {
        pipeline = new LocalLlmVisionPipeline(restClient, "qwen2.5-vl", 4096);
    }

    private void stubRestClient(Map<String, Object> response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);
    }

    private Map<String, Object> successResponse(String markdown) {
        return Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("role", "assistant", "content", markdown))
                )
        );
    }

    @Nested
    class RequestFormatting {

        @Test
        void callsChatCompletionsEndpoint() {
            stubRestClient(successResponse("# Hello"));

            pipeline.process(TEST_IMAGE);

            verify(requestBodyUriSpec).uri("/v1/chat/completions");
        }

        @Test
        @SuppressWarnings("unchecked")
        void sendsOpenAiCompatibleRequestBody() {
            stubRestClient(successResponse("# Hello"));

            pipeline.process(TEST_IMAGE);

            ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(requestBodySpec).body(bodyCaptor.capture());

            Map<String, Object> body = bodyCaptor.getValue();
            assertThat(body).containsEntry("model", "qwen2.5-vl");
            assertThat(body).containsEntry("max_tokens", 4096);

            var messages = (List<Map<String, Object>>) body.get("messages");
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst()).containsEntry("role", "user");

            var content = (List<Map<String, Object>>) messages.getFirst().get("content");
            assertThat(content).hasSize(2);

            // First block: image_url with data URI
            assertThat(content.get(0)).containsEntry("type", "image_url");
            var imageUrl = (Map<String, Object>) content.get(0).get("image_url");
            assertThat((String) imageUrl.get("url")).startsWith("data:image/png;base64,");

            // Second block: text prompt
            assertThat(content.get(1)).containsEntry("type", "text");
            assertThat((String) content.get(1).get("text")).contains("document-to-Markdown");
        }
    }

    @Nested
    class ResponseParsing {

        @Test
        void extractsMarkdownFromChoices() {
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
        void throwsOnEmptyChoices() {
            stubRestClient(Map.of("choices", List.of()));

            assertThatThrownBy(() -> pipeline.process(TEST_IMAGE))
                    .isInstanceOf(VisionApiException.class)
                    .hasMessageContaining("no choices");
        }

        @Test
        void throwsOnEmptyContent() {
            stubRestClient(Map.of(
                    "choices", List.of(
                            Map.of("message", Map.of("role", "assistant", "content", "  "))
                    )
            ));

            assertThatThrownBy(() -> pipeline.process(TEST_IMAGE))
                    .isInstanceOf(VisionApiException.class)
                    .hasMessageContaining("empty content");
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void wrapsConnectionErrorsInVisionApiException() {
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenThrow(
                    new org.springframework.web.client.ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> pipeline.process(TEST_IMAGE))
                    .isInstanceOf(VisionApiException.class)
                    .hasMessageContaining("Local LLM API call failed")
                    .hasCauseInstanceOf(org.springframework.web.client.ResourceAccessException.class);
        }
    }

    @Nested
    class Metadata {

        @Test
        void backendNameIsLocalLlm() {
            stubRestClient(successResponse("# Hello"));

            ProcessingResult result = pipeline.process(TEST_IMAGE);

            assertThat(result.metadata()).containsEntry("backend", "local-llm");
        }
    }
}
