package org.vaadin.photo2md.pipeline.domain;

import java.time.Duration;
import java.util.Map;

/**
 * Result of the full document-to-Markdown pipeline.
 *
 * @param markdown       the generated Markdown text
 * @param layout         the recognized document structure
 * @param processingTime how long the pipeline took
 * @param metadata       additional info (e.g., OCR engine used, confidence scores)
 */
public record ProcessingResult(
        String markdown,
        DocumentLayout layout,
        Duration processingTime,
        Map<String, String> metadata
) {

    public ProcessingResult {
        if (markdown == null) {
            markdown = "";
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
