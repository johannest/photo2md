package org.vaadin.photo2md.pipeline.domain;

/**
 * Position and size of a recognized element within the original image.
 *
 * @param x      left edge in pixels
 * @param y      top edge in pixels
 * @param width  width in pixels
 * @param height height in pixels
 */
public record BoundingBox(int x, int y, int width, int height) {

    public BoundingBox {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Width and height must be non-negative, got width=%d, height=%d"
                            .formatted(width, height));
        }
    }
}
