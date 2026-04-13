package org.vaadin.photo2md.pipeline.domain;

/**
 * A non-text image region (photo, diagram, badge, etc.).
 * Rendered as {@code [IMGn]()} placeholder in the Markdown output,
 * where n is the sequential image number in the document.
 *
 * @param imageData raw image bytes of the cropped region (may be null if not extracted)
 * @param altText   descriptive alt text (used as-is if provided; otherwise auto-numbered)
 * @param bounds    position in the original image
 */
public record ImageRegion(byte[] imageData, String altText, BoundingBox bounds)
        implements DocumentElement {

    public ImageRegion {
        if (altText == null || altText.isBlank()) {
            altText = "IMG";
        }
    }
}
