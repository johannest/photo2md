package org.vaadin.photo2md.pipeline;

import org.vaadin.photo2md.pipeline.domain.DocumentLayout;

/**
 * Analyzes a preprocessed document image and identifies distinct regions
 * (titles, paragraphs, lists, tables, code blocks, images).
 * <p>
 * At this stage the returned elements have bounding boxes and types set,
 * but text content may be empty — OCR fills it in later.
 */
public interface DocumentSegmenter {

    /**
     * @param preprocessedImage image bytes after preprocessing
     * @return a layout with elements ordered top-to-bottom
     */
    DocumentLayout segment(byte[] preprocessedImage);
}
