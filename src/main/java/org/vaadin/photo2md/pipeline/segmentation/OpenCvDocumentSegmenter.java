package org.vaadin.photo2md.pipeline.segmentation;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.vaadin.photo2md.pipeline.DocumentSegmenter;
import org.vaadin.photo2md.pipeline.domain.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * OpenCV-based document segmenter using horizontal projection profiling.
 * <p>
 * Approach:
 * <ol>
 *   <li>Decode image to grayscale</li>
 *   <li>Binarize with adaptive threshold (ink = foreground)</li>
 *   <li>Compute horizontal projection (sum of foreground pixels per row)</li>
 *   <li>Find contiguous row bands with non-zero projection (text regions)</li>
 *   <li>For each band, compute the bounding column range</li>
 *   <li>Return regions as Paragraph elements (classification refined later)</li>
 * </ol>
 * <p>
 * Phase 5 focuses on region detection — element type classification
 * (title vs list vs table) will be refined in Phase 8.
 */
@Component
public class OpenCvDocumentSegmenter implements DocumentSegmenter {

    private static final Logger log = LoggerFactory.getLogger(OpenCvDocumentSegmenter.class);

    /** Minimum gap (in rows) between text blocks to treat as separate elements. */
    private static final int MIN_GAP_ROWS = 8;

    /** Minimum height (in rows) for a region to be kept (filters noise). */
    private static final int MIN_REGION_HEIGHT = 5;

    @Override
    public DocumentLayout segment(byte[] preprocessedImage) {
        validateInput(preprocessedImage);

        Mat gray = decodeGrayscale(preprocessedImage);
        try {
            Mat binary = binarize(gray);
            try {
                List<DocumentElement> elements = detectRegions(binary, gray.cols(), gray.rows());
                log.debug("Segmented {} regions from {}x{} image",
                        elements.size(), gray.cols(), gray.rows());
                return new DocumentLayout(elements);
            } finally {
                binary.close();
            }
        } finally {
            gray.close();
        }
    }

    private void validateInput(byte[] image) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Input image must not be null or empty");
        }
    }

    private Mat decodeGrayscale(byte[] imageData) {
        try (BytePointer bp = new BytePointer(imageData)) {
            Mat buf = new Mat(new Size(1, imageData.length), CV_8UC1, bp);
            Mat gray = imdecode(buf, IMREAD_GRAYSCALE);
            buf.close();

            if (gray == null || gray.empty()) {
                throw new IllegalArgumentException(
                        "Unable to decode image — invalid or unsupported format");
            }
            return gray;
        }
    }

    private Mat binarize(Mat gray) {
        Mat binary = new Mat();
        // Invert so that text (dark) becomes foreground (white)
        adaptiveThreshold(gray, binary, 255,
                ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, 15, 10);
        return binary;
    }

    private List<DocumentElement> detectRegions(Mat binary, int imageWidth, int imageHeight) {
        int[] rowProjection = computeRowProjection(binary);
        List<int[]> bands = findTextBands(rowProjection);
        List<DocumentElement> elements = new ArrayList<>();

        for (int[] band : bands) {
            int yStart = band[0];
            int yEnd = band[1];
            int height = yEnd - yStart;

            if (height < MIN_REGION_HEIGHT) {
                continue;
            }

            // Find column range for this band
            int[] colRange = findColumnRange(binary, yStart, yEnd);
            int xStart = colRange[0];
            int xEnd = colRange[1];
            int width = xEnd - xStart;

            if (width <= 0) {
                continue;
            }

            // Clamp to image dimensions
            xStart = Math.max(0, xStart);
            yStart = Math.max(0, yStart);
            width = Math.min(width, imageWidth - xStart);
            height = Math.min(height, imageHeight - yStart);

            BoundingBox bounds = new BoundingBox(xStart, yStart, width, height);

            // Phase 5: classify everything as Paragraph for now
            // Phase 8 will add title/list/table/code classification
            elements.add(new Paragraph("", bounds));
        }

        // Sort top-to-bottom (should already be, but ensure)
        elements.sort(Comparator.comparingInt(e -> e.bounds().y()));

        return elements;
    }

    /**
     * Computes the horizontal projection: for each row, sum of foreground
     * (non-zero) pixels.
     */
    private int[] computeRowProjection(Mat binary) {
        int rows = binary.rows();
        int cols = binary.cols();
        int[] projection = new int[rows];

        try (UByteIndexer indexer = binary.createIndexer()) {
            for (int y = 0; y < rows; y++) {
                int sum = 0;
                for (int x = 0; x < cols; x++) {
                    if (indexer.get(y, x) > 0) {
                        sum++;
                    }
                }
                projection[y] = sum;
            }
        }
        return projection;
    }

    /**
     * Finds contiguous bands of rows with non-zero projection,
     * merging bands that are separated by less than {@link #MIN_GAP_ROWS}.
     *
     * @return list of [yStart, yEnd] pairs
     */
    private List<int[]> findTextBands(int[] projection) {
        List<int[]> rawBands = new ArrayList<>();
        int start = -1;

        for (int y = 0; y < projection.length; y++) {
            if (projection[y] > 0) {
                if (start < 0) {
                    start = y;
                }
            } else {
                if (start >= 0) {
                    rawBands.add(new int[]{start, y});
                    start = -1;
                }
            }
        }
        if (start >= 0) {
            rawBands.add(new int[]{start, projection.length});
        }

        // Merge bands separated by small gaps
        return mergeBands(rawBands);
    }

    private List<int[]> mergeBands(List<int[]> bands) {
        if (bands.isEmpty()) {
            return bands;
        }

        List<int[]> merged = new ArrayList<>();
        int[] current = bands.get(0);

        for (int i = 1; i < bands.size(); i++) {
            int[] next = bands.get(i);
            int gap = next[0] - current[1];
            if (gap < MIN_GAP_ROWS) {
                // Merge: extend current band
                current = new int[]{current[0], next[1]};
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    /**
     * Finds the leftmost and rightmost foreground pixels within a row range.
     */
    private int[] findColumnRange(Mat binary, int yStart, int yEnd) {
        int cols = binary.cols();
        int minX = cols;
        int maxX = 0;

        try (UByteIndexer indexer = binary.createIndexer()) {
            for (int y = yStart; y < yEnd; y++) {
                for (int x = 0; x < cols; x++) {
                    if (indexer.get(y, x) > 0) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                    }
                }
            }
        }

        if (minX >= maxX) {
            return new int[]{0, 0};
        }
        return new int[]{minX, maxX + 1};
    }
}
