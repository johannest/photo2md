package org.vaadin.photo2md.pipeline.ocr;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vaadin.photo2md.pipeline.OcrEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TesseractOcrEngine using real fixture images.
 * Requires Tesseract to be installed on the system.
 * <p>
 * See spec: src/test/resources/specs/03-ocr-extraction.md
 */
@SpringBootTest
@ActiveProfiles("tesseract")
class TesseractOcrEngineIT {

    @Autowired
    private OcrEngine ocrEngine;

    @Nested
    class EasyFixtures {

        @ParameterizedTest(name = "recognizes text from: {0}")
        @MethodSource("org.vaadin.photo2md.pipeline.ocr.TesseractOcrEngineIT#easyFixturesWithExpectedText")
        void recognizesExpectedTextFromFixture(String fixture, String expectedPhrase)
                throws IOException {
            byte[] image = loadFixture(fixture);
            String result = ocrEngine.recognize(image);

            assertThat(result)
                    .as("OCR of %s should contain '%s'", fixture, expectedPhrase)
                    .containsIgnoringCase(expectedPhrase);
        }
    }

    @Nested
    class OutputQuality {

        @Test
        void recognizesNonEmptyTextFromTitlesList() throws IOException {
            byte[] image = loadFixture("titles-list.png");
            String result = ocrEngine.recognize(image);
            assertThat(result).isNotBlank();
        }

        @Test
        void recognizesNonEmptyTextFromCodeBlocks() throws IOException {
            byte[] image = loadFixture("code-blocks-1.png");
            String result = ocrEngine.recognize(image);
            assertThat(result).isNotBlank();
        }

        @Test
        void recognizesNonEmptyTextFromTable() throws IOException {
            byte[] image = loadFixture("title-links-table.png");
            String result = ocrEngine.recognize(image);
            assertThat(result).isNotBlank();
        }

        @Test
        void recognizesNonEmptyTextFromMixed() throws IOException {
            byte[] image = loadFixture("img-titles-paragraphs-link-code.png");
            String result = ocrEngine.recognize(image);
            assertThat(result).isNotBlank();
        }
    }

    // --- Parameterized data ---

    static Stream<Arguments> easyFixturesWithExpectedText() {
        return Stream.of(
                // titles-list.png — should recognize key headings and items
                Arguments.of("titles-list.png", "photo2md"),
                Arguments.of("titles-list.png", "Architecture"),
                Arguments.of("titles-list.png", "Planned features"),
                Arguments.of("titles-list.png", "Vaadin"),
                Arguments.of("titles-list.png", "Java 21"),

                // code-blocks-1.png — should recognize heading and some code
                Arguments.of("code-blocks-1.png", "type-safe"),
                Arguments.of("code-blocks-1.png", "PersonService"),

                // title-links-table.png — should recognize table headers and content
                Arguments.of("title-links-table.png", "Vaadin Flow"),
                Arguments.of("title-links-table.png", "Branch"),
                Arguments.of("title-links-table.png", "Platform Version"),

                // img-titles-paragraphs-link-code.png — should recognize headings
                Arguments.of("img-titles-paragraphs-link-code.png", "TestBench"),
                Arguments.of("img-titles-paragraphs-link-code.png", "Releases"),
                Arguments.of("img-titles-paragraphs-link-code.png", "git clone")
        );
    }

    // --- Helpers ---

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
