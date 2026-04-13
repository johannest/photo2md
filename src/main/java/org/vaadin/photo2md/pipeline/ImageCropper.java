package org.vaadin.photo2md.pipeline;

import org.vaadin.photo2md.pipeline.domain.BoundingBox;

/**
 * Extracts a sub-region from an image given a bounding box.
 */
public interface ImageCropper {

    /**
     * @param imageBytes  full image as PNG/JPEG bytes
     * @param bounds      the region to extract
     * @return cropped image bytes (PNG), or empty array if cropping fails
     */
    byte[] crop(byte[] imageBytes, BoundingBox bounds);
}
