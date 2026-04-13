package org.vaadin.photo2md.pipeline;

/**
 * Preprocesses a raw document photograph for optimal OCR results.
 * Typical operations: grayscale conversion, contrast enhancement, deskewing.
 */
public interface ImagePreprocessor {

    /**
     * @param rawImage raw uploaded image bytes (PNG or JPEG)
     * @return preprocessed image bytes optimized for segmentation and OCR
     * @throws IllegalArgumentException if the input is null or empty
     */
    byte[] preprocess(byte[] rawImage);
}
