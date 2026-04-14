package org.vaadin.photo2md.pipeline.vision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Vision LLM pipeline using the Claude Messages API.
 * Sends the full document image and receives Markdown in a single call.
 */
public class ClaudeVisionPipeline extends VisionLlmPipeline {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVisionPipeline.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public ClaudeVisionPipeline(RestClient restClient, String model, int maxTokens) {
        this.restClient = restClient;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    protected String callVisionApi(String base64Image, String mediaType) {
        var imageContent = Map.of(
                "type", "image",
                "source", Map.of(
                        "type", "base64",
                        "media_type", mediaType,
                        "data", base64Image
                )
        );
        var textContent = Map.of(
                "type", "text",
                "text", systemPrompt()
        );

        var requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(imageContent, textContent)
                        )
                )
        );

        log.debug("Calling Claude API with model={}, maxTokens={}", model, maxTokens);

        @SuppressWarnings("unchecked") // JSON response structure is well-defined
        var response = restClient.post()
                .uri("/v1/messages")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractMarkdown(response);
    }

    @Override
    protected String backendName() {
        return "claude";
    }

    @SuppressWarnings("unchecked")
    private String extractMarkdown(Map<String, Object> response) {
        if (response == null) {
            throw new VisionApiException("Claude API returned null response");
        }

        var content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new VisionApiException("Claude API returned empty content");
        }

        var firstBlock = content.getFirst();
        var text = (String) firstBlock.get("text");
        if (text == null || text.isBlank()) {
            throw new VisionApiException("Claude API returned empty text in content block");
        }

        return text;
    }
}
