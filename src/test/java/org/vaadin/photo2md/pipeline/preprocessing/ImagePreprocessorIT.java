package org.vaadin.photo2md.pipeline.preprocessing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.photo2md.pipeline.ImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ImagePreprocessor using real fixture images.
 * Verifies preprocessing works on actual document screenshots.
 */
@SpringBootTest
class ImagePreprocessorIT {

    @Autowired
    private ImagePreprocessor preprocessor;

    @ParameterizedTest(name = "preprocesses fixture: {0}")
    @MethodSource("easyFixtures")
    void preprocessesFixtureImage(String fixtureName) throws IOException {
        byte[] input = loadFixture(fixtureName);
        byte[] result = preprocessor.preprocess(input);

        assertThat(result).isNotNull().isNotEmpty();

        BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(output).isNotNull();

        // Should be grayscale
        for (int x = 0; x < output.getWidth(); x += 50) {
            for (int y = 0; y < output.getHeight(); y += 50) {
                Color c = new Color(output.getRGB(x, y));
                assertThat(c.getRed())
                        .as("Pixel (%d,%d) should be grayscale", x, y)
                        .isEqualTo(c.getGreen())
                        .isEqualTo(c.getBlue());
            }
        }
    }

    @ParameterizedTest(name = "preserves dimensions: {0}")
    @MethodSource("easyFixtures")
    void preservesDimensionsForFixture(String fixtureName) throws IOException {
        byte[] input = loadFixture(fixtureName);
        BufferedImage inputImg = ImageIO.read(new ByteArrayInputStream(input));

        byte[] result = preprocessor.preprocess(input);
        BufferedImage outputImg = ImageIO.read(new ByteArrayInputStream(result));

        assertThat(outputImg.getWidth())
                .isBetween(
                        (int) (inputImg.getWidth() * 0.95),
                        (int) (inputImg.getWidth() * 1.05));
        assertThat(outputImg.getHeight())
                .isBetween(
                        (int) (inputImg.getHeight() * 0.95),
                        (int) (inputImg.getHeight() * 1.05));
    }

    static Stream<String> easyFixtures() {
        return Stream.of(
                "titles-list.png",
                "code-blocks-1.png",
                "title-links-table.png",
                "img-titles-paragraphs-link-code.png");
    }

    private byte[] loadFixture(String name) throws IOException {
        String path = "fixtures/easy/" + name;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(is)
                    .as("Fixture %s should exist on classpath", path)
                    .isNotNull();
            return is.readAllBytes();
        }
    }
}
