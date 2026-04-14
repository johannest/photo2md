package org.vaadin.photo2md.pipeline.vision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Vision LLM pipeline using a local OpenAI-compatible API (LM Studio, Ollama, etc.).
 * Sends the full document image and receives Markdown in a single call.
 * Keeps all data local — no cloud API calls.
 */
public class LocalLlmVisionPipeline extends VisionLlmPipeline {

    private static final Logger log = LoggerFactory.getLogger(LocalLlmVisionPipeline.class);

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public LocalLlmVisionPipeline(RestClient restClient, String model, int maxTokens) {
        this.restClient = restClient;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    protected String callVisionApi(String base64Image, String mediaType) {
        var imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of(
                        "url", "data:" + mediaType + ";base64," + base64Image
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

        log.debug("Calling local LLM API with model={}, maxTokens={}", model, maxTokens);

        try {
            @SuppressWarnings("unchecked")
            var response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            return extractMarkdown(response);
        } catch (VisionApiException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionApiException(
                    "Local LLM API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected String backendName() {
        return "local-llm";
    }

    @SuppressWarnings("unchecked")
    private String extractMarkdown(Map<String, Object> response) {
        if (response == null) {
            throw new VisionApiException("Local LLM API returned null response");
        }

        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new VisionApiException("Local LLM API returned no choices");
        }

        var message = (Map<String, Object>) choices.getFirst().get("message");
        if (message == null) {
            throw new VisionApiException("Local LLM API returned no message in choice");
        }

        var content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new VisionApiException("Local LLM API returned empty content");
        }

        return content;
    }
}
