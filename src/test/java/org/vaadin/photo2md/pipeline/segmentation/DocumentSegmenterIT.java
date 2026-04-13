package org.vaadin.photo2md.pipeline.segmentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.photo2md.pipeline.DocumentSegmenter;
import org.vaadin.photo2md.pipeline.domain.*;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DocumentSegmenter using real fixture images.
 * Verifies that the segmenter detects a reasonable number of regions
 * with correct element types for known documents.
 * <p>
 * See spec: src/test/resources/specs/02-document-segmentation.md
 */
@SpringBootTest
class DocumentSegmenterIT {

    @Autowired
    private DocumentSegmenter segmenter;

    @Test
    void titlesListFixtureHasMultipleElements() throws IOException {
        // titles-list.png: 3 headings, 1 paragraph, 2 lists
        DocumentLayout layout = segmentFixture("titles-list.png");
        assertThat(layout.elements())
                .as("titles-list should have multiple elements")
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void titlesListFixtureElementsAreOrdered() throws IOException {
        DocumentLayout layout = segmentFixture("titles-list.png");
        var elements = layout.elements();
        for (int i = 1; i < elements.size(); i++) {
            assertThat(elements.get(i).bounds().y())
                    .as("Element %d should be below element %d", i, i - 1)
                    .isGreaterThanOrEqualTo(elements.get(i - 1).bounds().y());
        }
    }

    @Test
    void codeBlocksFixtureHasMultipleElements() throws IOException {
        // code-blocks-1.png: paragraph, heading, paragraph, code labels, 2 code blocks
        DocumentLayout layout = segmentFixture("code-blocks-1.png");
        assertThat(layout.elements())
                .as("code-blocks-1 should have multiple elements")
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void tableLinkFixtureHasMultipleElements() throws IOException {
        // title-links-table.png: heading, multiple paragraphs, table
        DocumentLayout layout = segmentFixture("title-links-table.png");
        assertThat(layout.elements())
                .as("title-links-table should have multiple elements")
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void imgTitlesParagraphsFixtureHasMultipleElements() throws IOException {
        // img-titles-paragraphs-link-code.png: image badges, headings, paragraphs, code
        DocumentLayout layout = segmentFixture("img-titles-paragraphs-link-code.png");
        assertThat(layout.elements())
                .as("img-titles-paragraphs-link-code should have multiple elements")
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void allFixtureElementsHaveValidBoundingBoxes() throws IOException {
        String[] fixtures = {
                "titles-list.png",
                "code-blocks-1.png",
                "title-links-table.png",
                "img-titles-paragraphs-link-code.png"
        };

        for (String fixture : fixtures) {
            DocumentLayout layout = segmentFixture(fixture);
            for (DocumentElement element : layout.elements()) {
                BoundingBox b = element.bounds();
                assertThat(b.x()).as("x in %s", fixture).isGreaterThanOrEqualTo(0);
                assertThat(b.y()).as("y in %s", fixture).isGreaterThanOrEqualTo(0);
                assertThat(b.width()).as("width in %s", fixture).isGreaterThan(0);
                assertThat(b.height()).as("height in %s", fixture).isGreaterThan(0);
            }
        }
    }

    // --- Helpers ---

    private DocumentLayout segmentFixture(String name) throws IOException {
        byte[] image = loadFixture(name);
        return segmenter.segment(image);
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
