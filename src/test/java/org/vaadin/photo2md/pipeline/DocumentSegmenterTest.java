package org.vaadin.photo2md.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.vaadin.photo2md.pipeline.domain.*;
import org.vaadin.photo2md.pipeline.segmentation.OpenCvDocumentSegmenter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for DocumentSegmenter.
 * Written BEFORE the implementation — see spec 02-document-segmentation.md.
 * <p>
 * Uses programmatically generated images for deterministic unit testing.
 * Real fixture-based tests are in the integration test.
 */
class DocumentSegmenterTest {

    private DocumentSegmenter segmenter;

    @BeforeEach
    void setUp() {
        segmenter = new OpenCvDocumentSegmenter();
    }

    @Nested
    class InputValidation {

        @Test
        void nullInputThrowsException() {
            assertThatThrownBy(() -> segmenter.segment(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void emptyInputThrowsException() {
            assertThatThrownBy(() -> segmenter.segment(new byte[0]))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class BasicSegmentation {

        @Test
        void emptyWhiteImageReturnsEmptyLayout() throws IOException {
            byte[] blank = createBlankImage(400, 300);
            DocumentLayout layout = segmenter.segment(blank);
            assertThat(layout.elements()).isEmpty();
        }

        @Test
        void singleTextBlockDetected() throws IOException {
            byte[] image = createImageWithText(
                    400, 100,
                    new String[]{"Hello World"},
                    14, 0);
            DocumentLayout layout = segmenter.segment(image);
            assertThat(layout.elements()).isNotEmpty();
        }

        @Test
        void multipleBlocksSeparatedByWhitespace() throws IOException {
            // Create image with two text blocks separated by gap
            BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 200);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("First block of text here", 20, 30);
            // Leave gap (y=30 to y=120 is whitespace)
            g.drawString("Second block of text here", 20, 150);
            g.dispose();

            byte[] image = encodePng(img);
            DocumentLayout layout = segmenter.segment(image);
            assertThat(layout.elements()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    class ElementOrdering {

        @Test
        void elementsAreOrderedTopToBottom() throws IOException {
            BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 200);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.drawString("Top element", 20, 40);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("Bottom element", 20, 160);
            g.dispose();

            byte[] image = encodePng(img);
            DocumentLayout layout = segmenter.segment(image);

            if (layout.elements().size() >= 2) {
                int firstY = layout.elements().get(0).bounds().y();
                int lastY = layout.elements().get(layout.elements().size() - 1).bounds().y();
                assertThat(firstY).isLessThanOrEqualTo(lastY);
            }
        }
    }

    @Nested
    class BoundingBoxes {

        @Test
        void boundingBoxesAreNonNegative() throws IOException {
            byte[] image = createImageWithText(
                    400, 100, new String[]{"Test text"}, 14, 0);
            DocumentLayout layout = segmenter.segment(image);

            for (DocumentElement element : layout.elements()) {
                BoundingBox b = element.bounds();
                assertThat(b.x()).isGreaterThanOrEqualTo(0);
                assertThat(b.y()).isGreaterThanOrEqualTo(0);
                assertThat(b.width()).isGreaterThan(0);
                assertThat(b.height()).isGreaterThan(0);
            }
        }

        @Test
        void boundingBoxesDoNotExceedImageDimensions() throws IOException {
            int width = 400;
            int height = 100;
            byte[] image = createImageWithText(
                    width, height, new String[]{"Test text"}, 14, 0);
            DocumentLayout layout = segmenter.segment(image);

            for (DocumentElement element : layout.elements()) {
                BoundingBox b = element.bounds();
                assertThat(b.x() + b.width()).isLessThanOrEqualTo(width);
                assertThat(b.y() + b.height()).isLessThanOrEqualTo(height);
            }
        }
    }

    // --- Helpers ---

    private static byte[] createBlankImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return encodePng(img);
    }

    private static byte[] createImageWithText(int width, int height,
                                               String[] lines, int fontSize,
                                               int leftMargin) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int y = fontSize + 10;
        for (String line : lines) {
            g.drawString(line, 20 + leftMargin, y);
            y += fontSize + 4;
        }
        g.dispose();
        return encodePng(img);
    }

    private static byte[] encodePng(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
