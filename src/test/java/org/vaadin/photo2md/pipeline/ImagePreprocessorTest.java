package org.vaadin.photo2md.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.vaadin.photo2md.pipeline.preprocessing.OpenCvImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for ImagePreprocessor.
 * Written BEFORE the implementation — see spec 01-image-preprocessing.md.
 */
class ImagePreprocessorTest {

    private ImagePreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new OpenCvImagePreprocessor();
    }

    @Nested
    class InputValidation {

        @Test
        void nullInputThrowsException() {
            assertThatThrownBy(() -> preprocessor.preprocess(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void emptyInputThrowsException() {
            assertThatThrownBy(() -> preprocessor.preprocess(new byte[0]))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidImageDataThrowsException() {
            assertThatThrownBy(() -> preprocessor.preprocess(new byte[]{1, 2, 3}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class OutputProperties {

        @Test
        void outputIsNonNull() throws IOException {
            byte[] input = createColorTestImage(200, 100, Color.BLUE);
            byte[] result = preprocessor.preprocess(input);
            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        void outputIsValidImage() throws IOException {
            byte[] input = createColorTestImage(200, 100, Color.RED);
            byte[] result = preprocessor.preprocess(input);

            BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(output).isNotNull();
        }

        @Test
        void outputIsGrayscale() throws IOException {
            byte[] input = createColorTestImage(200, 100, Color.GREEN);
            byte[] result = preprocessor.preprocess(input);

            BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(output).isNotNull();

            // Sample pixels and verify R == G == B (grayscale)
            for (int x = 0; x < output.getWidth(); x += 20) {
                for (int y = 0; y < output.getHeight(); y += 20) {
                    Color c = new Color(output.getRGB(x, y));
                    assertThat(c.getRed())
                            .as("Pixel (%d,%d) should be grayscale", x, y)
                            .isEqualTo(c.getGreen())
                            .isEqualTo(c.getBlue());
                }
            }
        }

        @Test
        void outputDimensionsMatchInput() throws IOException {
            byte[] input = createColorTestImage(300, 200, Color.YELLOW);
            byte[] result = preprocessor.preprocess(input);

            BufferedImage inputImg = ImageIO.read(new ByteArrayInputStream(input));
            BufferedImage outputImg = ImageIO.read(new ByteArrayInputStream(result));

            assertThat(outputImg.getWidth())
                    .as("Width should match within 5%% tolerance")
                    .isBetween(
                            (int) (inputImg.getWidth() * 0.95),
                            (int) (inputImg.getWidth() * 1.05));
            assertThat(outputImg.getHeight())
                    .as("Height should match within 5%% tolerance")
                    .isBetween(
                            (int) (inputImg.getHeight() * 0.95),
                            (int) (inputImg.getHeight() * 1.05));
        }
    }

    @Nested
    class FormatSupport {

        @Test
        void acceptsPngInput() throws IOException {
            byte[] png = createTestImage(150, 100, "png");
            byte[] result = preprocessor.preprocess(png);
            assertThat(result).isNotEmpty();
        }

        @Test
        void acceptsJpegInput() throws IOException {
            byte[] jpeg = createTestImage(150, 100, "jpg");
            byte[] result = preprocessor.preprocess(jpeg);
            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    class ContrastEnhancement {

        @Test
        void lowContrastImageGetsEnhanced() throws IOException {
            // Create a low-contrast gray image with subtle text
            BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(140, 140, 140)); // medium gray background
            g.fillRect(0, 0, 200, 100);
            g.setColor(new Color(120, 120, 120)); // slightly darker text
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.drawString("Test", 50, 60);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            byte[] input = baos.toByteArray();

            byte[] result = preprocessor.preprocess(input);
            BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));

            // Compute min and max pixel values of input and output
            int[] inputRange = pixelRange(img);
            int[] outputRange = pixelRange(output);

            // After enhancement, the range (max - min) should be larger
            int inputSpan = inputRange[1] - inputRange[0];
            int outputSpan = outputRange[1] - outputRange[0];
            assertThat(outputSpan)
                    .as("Output contrast span should be >= input contrast span")
                    .isGreaterThanOrEqualTo(inputSpan);
        }
    }

    // --- Helpers ---

    private static byte[] createColorTestImage(int width, int height, Color color)
            throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        // Add some text to make it more realistic
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Hello World", 10, height / 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private static byte[] createTestImage(int width, int height, String format)
            throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Test text for OCR", 10, height / 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    private static int[] pixelRange(BufferedImage img) {
        int min = 255;
        int max = 0;
        for (int x = 0; x < img.getWidth(); x += 5) {
            for (int y = 0; y < img.getHeight(); y += 5) {
                Color c = new Color(img.getRGB(x, y));
                int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                min = Math.min(min, gray);
                max = Math.max(max, gray);
            }
        }
        return new int[]{min, max};
    }
}
