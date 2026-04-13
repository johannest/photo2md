package org.vaadin.photo2md.pipeline.preprocessing;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.vaadin.photo2md.pipeline.ImageCropper;
import org.vaadin.photo2md.pipeline.domain.BoundingBox;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;

/**
 * OpenCV-based implementation that crops a bounding box region from an image.
 */
@Component
public class OpenCvImageCropper implements ImageCropper {

    private static final Logger log = LoggerFactory.getLogger(OpenCvImageCropper.class);

    @Override
    public byte[] crop(byte[] imageBytes, BoundingBox bounds) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new byte[0];
        }

        try (BytePointer bp = new BytePointer(imageBytes)) {
            Mat buf = new Mat(new Size(1, imageBytes.length), CV_8UC1, bp);
            Mat image = imdecode(buf, IMREAD_COLOR);
            buf.close();

            if (image == null || image.empty()) {
                return new byte[0];
            }

            try {
                int x = Math.max(0, bounds.x());
                int y = Math.max(0, bounds.y());
                int width = Math.min(bounds.width(), image.cols() - x);
                int height = Math.min(bounds.height(), image.rows() - y);

                if (width <= 0 || height <= 0) {
                    return new byte[0];
                }

                Rect roi = new Rect(x, y, width, height);
                Mat cropped = new Mat(image, roi);
                try {
                    return encodePng(cropped);
                } finally {
                    cropped.close();
                }
            } finally {
                image.close();
            }
        }
    }

    private byte[] encodePng(Mat image) {
        int maxSize = (int) (image.total() * image.elemSize()) + 1024;
        byte[] buf = new byte[maxSize];
        imencode(".png", image, buf);

        int pngEnd = findPngEnd(buf);
        byte[] result = new byte[pngEnd];
        System.arraycopy(buf, 0, result, 0, pngEnd);
        return result;
    }

    private int findPngEnd(byte[] data) {
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
        return data.length;
    }
}
