package org.vaadin.photo2md.pipeline;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OcrEngine interface contract.
 * Uses a simple stub to verify contract expectations.
 * Real OCR backends are tested in integration tests.
 * <p>
 * See spec: src/test/resources/specs/03-ocr-extraction.md
 */
class OcrEngineTest {

    /**
     * Minimal stub that returns empty string for any input.
     * Used to verify the interface contract only.
     */
    private final OcrEngine stubEngine = imageRegion -> {
        if (imageRegion == null || imageRegion.length == 0) {
            throw new IllegalArgumentException("Image region must not be null or empty");
        }
        return "";
    };

    @Nested
    class InterfaceContract {

        @Test
        void recognizeReturnsStringForValidInput() throws IOException {
            byte[] image = createSimpleTextImage("Hello");
            String result = stubEngine.recognize(image);
            assertThat(result).isNotNull();
        }

        @Test
        void recognizeReturnsEmptyForBlankRegion() throws IOException {
            byte[] image = createBlankImage(100, 50);
            String result = stubEngine.recognize(image);
            assertThat(result).isNotNull();
        }

        @Test
        void recognizeRejectsNull() {
            assertThatThrownBy(() -> stubEngine.recognize(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void recognizeRejectsEmptyArray() {
            assertThatThrownBy(() -> stubEngine.recognize(new byte[0]))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // --- Helpers ---

    static byte[] createSimpleTextImage(String text) throws IOException {
        BufferedImage img = new BufferedImage(300, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 300, 60);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString(text, 10, 40);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    static byte[] createBlankImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
