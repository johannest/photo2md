package org.vaadin.photo2md.pipeline.vision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.photo2md.pipeline.DocumentPipeline;
import org.vaadin.photo2md.pipeline.domain.DocumentLayout;
import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Base class for vision LLM pipeline backends that convert a document image
 * to Markdown in a single API call — bypassing preprocessing, segmentation,
 * and per-region OCR entirely.
 */
public abstract class VisionLlmPipeline implements DocumentPipeline {

    private static final Logger log = LoggerFactory.getLogger(VisionLlmPipeline.class);

    @Override
    public final ProcessingResult process(byte[] rawImage) {
        validateInput(rawImage);

        Instant start = Instant.now();
        log.info("{}: processing image ({} bytes)", backendName(), rawImage.length);

        String base64Image = Base64.getEncoder().encodeToString(rawImage);
        String mediaType = detectMediaType(rawImage);

        String markdown = callVisionApi(base64Image, mediaType);

        // Strip wrapping code fence if the model added one despite instructions
        markdown = stripMarkdownFence(markdown);

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("{}: completed in {}ms, {} characters of Markdown",
                backendName(), elapsed.toMillis(), markdown.length());

        return new ProcessingResult(
                markdown,
                new DocumentLayout(List.of()),
                elapsed,
                Map.of(
                        "backend", backendName(),
                        "processingTimeMs", String.valueOf(elapsed.toMillis())
                )
        );
    }

    /**
     * Calls the vision LLM API with a base64-encoded image and returns raw Markdown.
     *
     * @param base64Image the image encoded as base64
     * @param mediaType   the image MIME type (e.g., "image/png")
     * @return the Markdown text extracted from the image
     * @throws VisionApiException if the API call fails
     */
    protected abstract String callVisionApi(String base64Image, String mediaType);

    /**
     * @return a human-readable name for this backend (used in logs and metadata)
     */
    protected abstract String backendName();

    /**
     * @return the shared system prompt for vision-to-Markdown conversion
     */
    protected String systemPrompt() {
        return VisionPrompts.SYSTEM_PROMPT;
    }

    private void validateInput(byte[] rawImage) {
        if (rawImage == null) {
            throw new IllegalArgumentException("Input image must not be null");
        }
        if (rawImage.length == 0) {
            throw new IllegalArgumentException("Input image must not be empty");
        }
    }

    static String detectMediaType(byte[] image) {
        if (image.length >= 8
                && image[0] == (byte) 0x89
                && image[1] == (byte) 0x50) {
            return "image/png";
        }
        if (image.length >= 3
                && image[0] == (byte) 0xFF
                && image[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        return "image/png"; // default
    }

    static String stripMarkdownFence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        if (trimmed.startsWith("```markdown") || trimmed.startsWith("```md")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0 && trimmed.endsWith("```")) {
                return trimmed.substring(firstNewline + 1, trimmed.length() - 3).strip();
            }
        }
        if (trimmed.startsWith("```") && trimmed.endsWith("```") && trimmed.length() > 6) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                return trimmed.substring(firstNewline + 1, trimmed.length() - 3).strip();
            }
        }
        return trimmed;
    }
}
