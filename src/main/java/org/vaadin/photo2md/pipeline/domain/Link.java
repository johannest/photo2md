package org.vaadin.photo2md.pipeline.domain;

/**
 * A standalone hyperlink element.
 * <p>
 * Rendering rule:
 * <ul>
 *   <li>If the link text looks like a URL → {@code [url](url)}</li>
 *   <li>Otherwise → {@code [text]()} (empty URL, since we can't resolve URLs from a photo)</li>
 * </ul>
 *
 * @param text   visible link text
 * @param url    resolved URL (may be empty)
 * @param bounds position in the original image
 */
public record Link(String text, String url, BoundingBox bounds) implements DocumentElement {

    public Link {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Link text must not be blank");
        }
        if (url == null) {
            url = "";
        }
    }
}
