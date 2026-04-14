package org.vaadin.photo2md.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.vaadin.photo2md.pipeline.vision.ClaudeVisionPipeline;
import org.vaadin.photo2md.pipeline.vision.LocalLlmVisionPipeline;

/**
 * Configuration for vision LLM pipeline backends (Claude API and local LLM).
 */
@Configuration
public class VisionPipelineConfiguration {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 300_000; // 5 min — LLM inference can be slow

    @Bean
    @ConditionalOnProperty(name = "photo2md.pipeline.mode", havingValue = "claude")
    public ClaudeVisionPipeline claudeVisionPipeline(
            @Value("${photo2md.claude.api-key}") String apiKey,
            @Value("${photo2md.claude.model:claude-sonnet-4-20250514}") String model,
            @Value("${photo2md.claude.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${photo2md.claude.max-tokens:4096}") int maxTokens) {

        RestClient restClient = RestClient.builder()
                .requestFactory(createRequestFactory())
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        return new ClaudeVisionPipeline(restClient, model, maxTokens);
    }

    @Bean
    @ConditionalOnProperty(name = "photo2md.pipeline.mode", havingValue = "local-llm")
    public LocalLlmVisionPipeline localLlmVisionPipeline(
            @Value("${photo2md.local-llm.base-url:http://127.0.0.1:1234}") String baseUrl,
            @Value("${photo2md.local-llm.model:local-model}") String model,
            @Value("${photo2md.local-llm.api-key:not-needed}") String apiKey,
            @Value("${photo2md.local-llm.max-tokens:4096}") int maxTokens) {

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(createRequestFactory())
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank() && !"not-needed".equals(apiKey)) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        return new LocalLlmVisionPipeline(builder.build(), model, maxTokens);
    }

    private SimpleClientHttpRequestFactory createRequestFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }
}
