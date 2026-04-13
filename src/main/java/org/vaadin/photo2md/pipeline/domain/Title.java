package org.vaadin.photo2md.pipeline.domain;

/**
 * A heading element (h1–h6).
 *
 * @param level  heading level 1–6 (1 = largest)
 * @param text   the heading text (may contain inline styles)
 * @param bounds position in the original image
 */
public record Title(int level, String text, BoundingBox bounds) implements DocumentElement {

    public Title {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Title level must be 1–6, got " + level);
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Title text must not be blank");
        }
    }
}
