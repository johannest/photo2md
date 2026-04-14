package org.vaadin.photo2md.pipeline.vision;

/**
 * Thrown when a vision LLM API call fails.
 */
public class VisionApiException extends RuntimeException {

    private final int statusCode;

    public VisionApiException(String message) {
        this(message, 0, null);
    }

    public VisionApiException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    public VisionApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public VisionApiException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public int statusCode() {
        return statusCode;
    }
}
