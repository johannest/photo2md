package org.vaadin.photo2md.pipeline.domain;

/**
 * A block of body text. The text may contain inline Markdown formatting
 * (bold, italic, monospace, links).
 *
 * @param text   paragraph content (may include inline Markdown)
 * @param bounds position in the original image
 */
public record Paragraph(String text, BoundingBox bounds) implements DocumentElement {

    public Paragraph {
        if (text == null) {
            throw new IllegalArgumentException("Paragraph text must not be null");
        }
    }
}
