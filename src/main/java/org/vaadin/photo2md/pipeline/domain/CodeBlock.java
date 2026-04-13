package org.vaadin.photo2md.pipeline.domain;

/**
 * A block of code. Always rendered as a fenced code block in Markdown.
 *
 * @param language optional language identifier (e.g., "java", "ts"); empty string if unknown
 * @param code     the code content (whitespace and indentation preserved)
 * @param bounds   position in the original image
 */
public record CodeBlock(String language, String code, BoundingBox bounds) implements DocumentElement {

    public CodeBlock {
        if (language == null) {
            language = "";
        }
        if (code == null) {
            throw new IllegalArgumentException("CodeBlock code must not be null");
        }
    }
}
