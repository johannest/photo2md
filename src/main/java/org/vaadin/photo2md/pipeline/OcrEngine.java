package org.vaadin.photo2md.pipeline;

/**
 * Extracts text from a single image region using optical character recognition.
 */
public interface OcrEngine {

    /**
     * @param imageRegion cropped image bytes of a single document region
     * @return the recognized text (may be empty for non-text regions)
     */
    String recognize(byte[] imageRegion);
}
