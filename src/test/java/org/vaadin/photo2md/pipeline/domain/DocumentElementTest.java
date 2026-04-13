package org.vaadin.photo2md.pipeline.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for domain record construction, validation, and sealed interface exhaustiveness.
 */
class DocumentElementTest {

    private static final BoundingBox DUMMY = new BoundingBox(0, 0, 100, 20);

    @Nested
    class BoundingBoxTests {

        @Test
        void validBoundingBox() {
            var box = new BoundingBox(10, 20, 100, 50);
            assertThat(box.x()).isEqualTo(10);
            assertThat(box.y()).isEqualTo(20);
            assertThat(box.width()).isEqualTo(100);
            assertThat(box.height()).isEqualTo(50);
        }

        @Test
        void zeroDimensionsAllowed() {
            var box = new BoundingBox(0, 0, 0, 0);
            assertThat(box.width()).isZero();
        }

        @Test
        void negativeDimensionsRejected() {
            assertThatThrownBy(() -> new BoundingBox(0, 0, -1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BoundingBox(0, 0, 10, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TitleTests {

        @Test
        void validTitle() {
            var title = new Title(1, "Hello", DUMMY);
            assertThat(title.level()).isEqualTo(1);
            assertThat(title.text()).isEqualTo("Hello");
        }

        @Test
        void levelOutOfRangeRejected() {
            assertThatThrownBy(() -> new Title(0, "Bad", DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Title(7, "Bad", DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankTextRejected() {
            assertThatThrownBy(() -> new Title(1, "", DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Title(1, "  ", DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ParagraphTests {

        @Test
        void validParagraph() {
            var p = new Paragraph("Some text.", DUMMY);
            assertThat(p.text()).isEqualTo("Some text.");
        }

        @Test
        void nullTextRejected() {
            assertThatThrownBy(() -> new Paragraph(null, DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void emptyTextAllowed() {
            var p = new Paragraph("", DUMMY);
            assertThat(p.text()).isEmpty();
        }
    }

    @Nested
    class ListBlockTests {

        @Test
        void validOrderedList() {
            var list = new ListBlock(ListBlock.ListType.ORDERED,
                    List.of("A", "B"), DUMMY);
            assertThat(list.type()).isEqualTo(ListBlock.ListType.ORDERED);
            assertThat(list.items()).containsExactly("A", "B");
            assertThat(list.children()).isEmpty();
        }

        @Test
        void emptyItemsRejected() {
            assertThatThrownBy(() -> new ListBlock(
                    ListBlock.ListType.UNORDERED, List.of(), DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullChildrenDefaultsToEmptyList() {
            var list = new ListBlock(ListBlock.ListType.ORDERED,
                    List.of("A"), null, DUMMY);
            assertThat(list.children()).isEmpty();
        }
    }

    @Nested
    class TableTests {

        @Test
        void validTable() {
            var table = new Table(List.of("H1", "H2"),
                    List.of(List.of("a", "b")), DUMMY);
            assertThat(table.headers()).containsExactly("H1", "H2");
            assertThat(table.rows()).hasSize(1);
        }

        @Test
        void emptyHeadersRejected() {
            assertThatThrownBy(() -> new Table(List.of(), List.of(), DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullRowsDefaultsToEmptyList() {
            var table = new Table(List.of("H"), null, DUMMY);
            assertThat(table.rows()).isEmpty();
        }
    }

    @Nested
    class CodeBlockTests {

        @Test
        void validCodeBlock() {
            var cb = new CodeBlock("java", "int x = 1;", DUMMY);
            assertThat(cb.language()).isEqualTo("java");
            assertThat(cb.code()).isEqualTo("int x = 1;");
        }

        @Test
        void nullLanguageDefaultsToEmpty() {
            var cb = new CodeBlock(null, "code", DUMMY);
            assertThat(cb.language()).isEmpty();
        }

        @Test
        void nullCodeRejected() {
            assertThatThrownBy(() -> new CodeBlock("java", null, DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ImageRegionTests {

        @Test
        void validImageRegion() {
            var img = new ImageRegion(new byte[]{1, 2}, "logo", DUMMY);
            assertThat(img.altText()).isEqualTo("logo");
        }

        @Test
        void nullAltTextDefaultsToIMG() {
            var img = new ImageRegion(null, null, DUMMY);
            assertThat(img.altText()).isEqualTo("IMG");
        }

        @Test
        void blankAltTextDefaultsToIMG() {
            var img = new ImageRegion(null, "  ", DUMMY);
            assertThat(img.altText()).isEqualTo("IMG");
        }
    }

    @Nested
    class LinkTests {

        @Test
        void validLink() {
            var link = new Link("docs", "https://example.com", DUMMY);
            assertThat(link.text()).isEqualTo("docs");
            assertThat(link.url()).isEqualTo("https://example.com");
        }

        @Test
        void blankTextRejected() {
            assertThatThrownBy(() -> new Link("", "", DUMMY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullUrlDefaultsToEmpty() {
            var link = new Link("docs", null, DUMMY);
            assertThat(link.url()).isEmpty();
        }
    }

    @Nested
    class SealedInterfaceTests {

        @Test
        void allPermitsAreDocumentElements() {
            // Verify all seven types implement DocumentElement
            List<DocumentElement> elements = List.of(
                    new Title(1, "T", DUMMY),
                    new Paragraph("P", DUMMY),
                    new ListBlock(ListBlock.ListType.ORDERED, List.of("i"), DUMMY),
                    new Table(List.of("H"), List.of(), DUMMY),
                    new CodeBlock("", "c", DUMMY),
                    new ImageRegion(null, "img", DUMMY),
                    new Link("L", "", DUMMY));

            assertThat(elements).hasSize(7);
            elements.forEach(e -> {
                assertThat(e).isInstanceOf(DocumentElement.class);
                assertThat(e.bounds()).isEqualTo(DUMMY);
            });
        }

        @Test
        void patternMatchingIsExhaustive() {
            // This test verifies the sealed interface supports exhaustive switch
            DocumentElement element = new Title(1, "Test", DUMMY);
            String result = switch (element) {
                case Title t -> "title:" + t.level();
                case Paragraph p -> "paragraph";
                case ListBlock l -> "list:" + l.type();
                case Table t -> "table";
                case CodeBlock c -> "code:" + c.language();
                case ImageRegion i -> "image";
                case Link l -> "link:" + l.text();
            };
            assertThat(result).isEqualTo("title:1");
        }
    }
}
