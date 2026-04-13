package org.vaadin.photo2md.pipeline.preprocessing;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.vaadin.photo2md.pipeline.ImagePreprocessor;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * OpenCV-based image preprocessor that converts a raw document photo
 * to an optimized grayscale image for OCR.
 * <p>
 * Pipeline: decode → grayscale → CLAHE contrast enhancement → encode PNG.
 */
@Component
public class OpenCvImagePreprocessor implements ImagePreprocessor {

    private static final Logger log = LoggerFactory.getLogger(OpenCvImagePreprocessor.class);

    @Override
    public byte[] preprocess(byte[] rawImage) {
        validateInput(rawImage);

        Mat color = decodeImage(rawImage);
        try {
            Mat gray = toGrayscale(color);
            try {
                Mat enhanced = enhanceContrast(gray);
                try {
                    return encodePng(enhanced);
                } finally {
                    enhanced.close();
                }
            } finally {
                gray.close();
            }
        } finally {
            color.close();
        }
    }

    private void validateInput(byte[] rawImage) {
        if (rawImage == null || rawImage.length == 0) {
            throw new IllegalArgumentException("Input image must not be null or empty");
        }
    }

    private Mat decodeImage(byte[] rawImage) {
        try (BytePointer bp = new BytePointer(rawImage)) {
            Mat buf = new Mat(new Size(1, rawImage.length), CV_8UC1, bp);
            Mat decoded = imdecode(buf, IMREAD_COLOR);
            buf.close();

            if (decoded == null || decoded.empty()) {
                throw new IllegalArgumentException(
                        "Unable to decode image — invalid or unsupported format");
            }

            log.debug("Decoded image: {}x{}, channels={}",
                    decoded.cols(), decoded.rows(), decoded.channels());
            return decoded;
        }
    }

    private Mat toGrayscale(Mat color) {
        if (color.channels() == 1) {
            return color.clone();
        }
        Mat gray = new Mat();
        cvtColor(color, gray, COLOR_BGR2GRAY);
        return gray;
    }

    private Mat enhanceContrast(Mat gray) {
        Mat enhanced = new Mat();
        var clahe = createCLAHE(2.0, new Size(8, 8));
        clahe.apply(gray, enhanced);
        return enhanced;
    }

    private byte[] encodePng(Mat grayImage) {
        // Convert single-channel to 3-channel for consistent RGB PNG output
        Mat bgr = new Mat();
        cvtColor(grayImage, bgr, COLOR_GRAY2BGR);

        try {
            // Allocate buffer large enough for PNG encoding
            int maxSize = (int) (bgr.total() * bgr.elemSize()) + 1024;
            byte[] buf = new byte[maxSize];
            imencode(".png", bgr, buf);

            // imencode writes into buf; find actual PNG size by looking for IEND
            int pngEnd = findPngEnd(buf);
            byte[] result = new byte[pngEnd];
            System.arraycopy(buf, 0, result, 0, pngEnd);

            log.debug("Encoded PNG: {} bytes", result.length);
            return result;
        } finally {
            bgr.close();
        }
    }

    private int findPngEnd(byte[] data) {
        // PNG ends with IEND chunk: 00 00 00 00 49 45 4E 44 AE 42 60 82
        byte[] iend = {(byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44,
                       (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82};
        for (int i = data.length - iend.length; i >= 0; i--) {
            boolean match = true;
            for (int j = 0; j < iend.length; j++) {
                if (data[i + j] != iend[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i + iend.length;
            }
        }
        return data.length; // fallback: return full buffer
    }
}
