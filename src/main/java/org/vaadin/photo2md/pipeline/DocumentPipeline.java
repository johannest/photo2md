package org.vaadin.photo2md.pipeline;

import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

/**
 * Converts a raw document image to Markdown.
 * <p>
 * Implementations may use different strategies:
 * <ul>
 *   <li>{@code staged} — classical pipeline (preprocess → segment → OCR → generate)</li>
 *   <li>{@code claude} — single-shot vision LLM via Claude API</li>
 *   <li>{@code local-llm} — single-shot vision LLM via local OpenAI-compatible API</li>
 * </ul>
 * Selected at runtime by the {@code photo2md.pipeline.mode} property.
 */
public interface DocumentPipeline {

    /**
     * Processes a raw document image and returns Markdown with metadata.
     *
     * @param rawImage the uploaded image bytes (PNG or JPEG)
     * @return the processing result containing Markdown, layout, timing, and metadata
     * @throws IllegalArgumentException if the input is null or empty
     */
    ProcessingResult process(byte[] rawImage);
}
