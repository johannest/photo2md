package org.vaadin.photo2md.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.vaadin.photo2md.pipeline.domain.*;
import org.vaadin.photo2md.pipeline.markdown.DefaultMarkdownGenerator;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for MarkdownGenerator.
 * Written BEFORE the implementation — see spec 04-markdown-generation.md.
 */
class MarkdownGeneratorTest {

    private static final BoundingBox DUMMY = new BoundingBox(0, 0, 100, 20);

    private MarkdownGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DefaultMarkdownGenerator();
    }

    // --- Titles ---

    @Nested
    class Titles {

        @ParameterizedTest(name = "H{0} produces {0} hashes")
        @CsvSource({"1", "2", "3", "4", "5", "6"})
        void titleLevelProducesCorrectHashes(int level) {
            var layout = layoutOf(new Title(level, "Hello World", DUMMY));
            String md = generator.generate(layout);
            String expected = "#".repeat(level) + " Hello World";
            assertThat(md.trim()).isEqualTo(expected);
        }

        @Test
        void titleWithInlineStyles() {
            var layout = layoutOf(new Title(2, "The **bold** title", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).isEqualTo("## The **bold** title");
        }
    }

    // --- Paragraphs ---

    @Nested
    class Paragraphs {

        @Test
        void plainParagraph() {
            var layout = layoutOf(new Paragraph("Lorem ipsum dolor sit amet.", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).isEqualTo("Lorem ipsum dolor sit amet.");
        }

        @Test
        void paragraphWithBold() {
            var layout = layoutOf(new Paragraph("This is **important** text.", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).contains("**important**");
        }

        @Test
        void paragraphWithItalic() {
            var layout = layoutOf(new Paragraph("This is *emphasized* text.", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).contains("*emphasized*");
        }

        @Test
        void paragraphWithInlineCode() {
            var layout = layoutOf(new Paragraph("Use the `println` method.", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).contains("`println`");
        }

        @Test
        void paragraphWithLink() {
            var layout = layoutOf(new Paragraph(
                    "See [the documentation]() for details.", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).contains("[the documentation]()");
        }
    }

    // --- Lists ---

    @Nested
    class Lists {

        @Test
        void orderedList() {
            var layout = layoutOf(new ListBlock(
                    ListBlock.ListType.ORDERED,
                    List.of("First", "Second", "Third"),
                    DUMMY));
            String md = generator.generate(layout);
            assertThat(md).contains("1. First");
            assertThat(md).contains("2. Second");
            assertThat(md).contains("3. Third");
        }

        @Test
        void unorderedList() {
            var layout = layoutOf(new ListBlock(
                    ListBlock.ListType.UNORDERED,
                    List.of("Alpha", "Beta"),
                    DUMMY));
            String md = generator.generate(layout);
            assertThat(md).contains("* Alpha");
            assertThat(md).contains("* Beta");
        }

        @Test
        void nestedList() {
            var nested = new ListBlock(
                    ListBlock.ListType.UNORDERED,
                    List.of("Sub-a", "Sub-b"),
                    DUMMY);
            var outer = new ListBlock(
                    ListBlock.ListType.ORDERED,
                    List.of("Item 1", "Item 2"),
                    Arrays.asList(null, nested),  // nested under item 2
                    DUMMY);
            var layout = layoutOf(outer);
            String md = generator.generate(layout);

            assertThat(md).contains("1. Item 1");
            assertThat(md).contains("2. Item 2");
            // Nested items should be indented
            assertThat(md).contains("   * Sub-a");
            assertThat(md).contains("   * Sub-b");
        }

        @Test
        void listItemsWithInlineFormatting() {
            var layout = layoutOf(new ListBlock(
                    ListBlock.ListType.UNORDERED,
                    List.of("**bold item**", "*italic item*"),
                    DUMMY));
            String md = generator.generate(layout);
            assertThat(md).contains("* **bold item**");
            assertThat(md).contains("* *italic item*");
        }
    }

    // --- Tables ---

    @Nested
    class Tables {

        @Test
        void simpleTable() {
            var layout = layoutOf(new Table(
                    List.of("Name", "Age"),
                    List.of(
                            List.of("Alice", "30"),
                            List.of("Bob", "25")),
                    DUMMY));
            String md = generator.generate(layout);

            assertThat(md).contains("| Name | Age |");
            assertThat(md).contains("|---");
            assertThat(md).contains("| Alice | 30 |");
            assertThat(md).contains("| Bob | 25 |");
        }

        @Test
        void tableWithInlineLinks() {
            var layout = layoutOf(new Table(
                    List.of("[Platform Version]()", "Details"),
                    List.of(List.of("25.0", "Latest release")),
                    DUMMY));
            String md = generator.generate(layout);

            assertThat(md).contains("| [Platform Version]() | Details |");
        }

        @Test
        void tableWithNoRows() {
            var layout = layoutOf(new Table(
                    List.of("Header"),
                    List.of(),
                    DUMMY));
            String md = generator.generate(layout);

            assertThat(md).contains("| Header |");
            assertThat(md).contains("|---");
        }
    }

    // --- Code Blocks ---

    @Nested
    class CodeBlocks {

        @Test
        void fencedCodeBlockWithLanguage() {
            var layout = layoutOf(new CodeBlock("java",
                    "public class Foo {\n}", DUMMY));
            String md = generator.generate(layout);

            assertThat(md).contains("```java");
            assertThat(md).contains("public class Foo {");
            assertThat(md).contains("}");
            assertThat(md).contains("```");
        }

        @Test
        void fencedCodeBlockWithoutLanguage() {
            var layout = layoutOf(new CodeBlock("",
                    "git clone repo\ncd repo", DUMMY));
            String md = generator.generate(layout);

            assertThat(md).contains("```\n");
            assertThat(md).contains("git clone repo");
            // Should NOT start with ```<language>
            assertThat(md).doesNotContain("``` ");
        }

        @Test
        void codeBlockPreservesWhitespace() {
            String code = "  if (x) {\n    return y;\n  }";
            var layout = layoutOf(new CodeBlock("ts", code, DUMMY));
            String md = generator.generate(layout);

            assertThat(md).contains("  if (x) {");
            assertThat(md).contains("    return y;");
        }
    }

    // --- Image Regions ---

    @Nested
    class ImageRegions {

        @Test
        void singleImagePlaceholder() {
            var layout = layoutOf(new ImageRegion(null, "IMG1", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).isEqualTo("[IMG1]()");
        }

        @Test
        void multipleImagesNumberedSequentially() {
            var layout = new DocumentLayout(List.of(
                    new ImageRegion(null, "IMG1", DUMMY),
                    new Paragraph("Between images.", DUMMY),
                    new ImageRegion(null, "IMG2", DUMMY)));
            String md = generator.generate(layout);

            assertThat(md).contains("[IMG1]()");
            assertThat(md).contains("[IMG2]()");
        }

        @Test
        void imageWithDefaultAltText() {
            // When altText defaults to "IMG", generator should auto-number
            var layout = new DocumentLayout(List.of(
                    new ImageRegion(null, null, DUMMY),
                    new ImageRegion(null, null, DUMMY)));
            String md = generator.generate(layout);

            assertThat(md).contains("[IMG1]()");
            assertThat(md).contains("[IMG2]()");
        }
    }

    // --- Links ---

    @Nested
    class Links {

        @Test
        void linkWithNonUrlText() {
            var layout = layoutOf(new Link("the documentation", "", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).isEqualTo("[the documentation]()");
        }

        @Test
        void linkWhereTextIsUrl() {
            var layout = layoutOf(new Link(
                    "https://vaadin.com/docs", "https://vaadin.com/docs", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).isEqualTo(
                    "[https://vaadin.com/docs](https://vaadin.com/docs)");
        }

        @Test
        void linkWithHttpTextButEmptyUrl() {
            // Text looks like URL but no explicit url set — should still use text as url
            var layout = layoutOf(new Link(
                    "https://github.com/vaadin/hilla", "", DUMMY));
            String md = generator.generate(layout);
            assertThat(md.trim()).isEqualTo(
                    "[https://github.com/vaadin/hilla](https://github.com/vaadin/hilla)");
        }
    }

    // --- Mixed Document ---

    @Nested
    class MixedDocument {

        @Test
        void elementsAreRenderedInOrderWithBlankLines() {
            var layout = new DocumentLayout(List.of(
                    new Title(1, "Report", DUMMY),
                    new Paragraph("Introduction text.", DUMMY),
                    new ListBlock(ListBlock.ListType.ORDERED,
                            List.of("Step A", "Step B"), DUMMY)));
            String md = generator.generate(layout);

            assertThat(md).contains("# Report");
            assertThat(md).contains("Introduction text.");
            assertThat(md).contains("1. Step A");
            assertThat(md).contains("2. Step B");

            // Verify ordering: title before paragraph before list
            int titleIdx = md.indexOf("# Report");
            int paraIdx = md.indexOf("Introduction text.");
            int listIdx = md.indexOf("1. Step A");
            assertThat(titleIdx).isLessThan(paraIdx);
            assertThat(paraIdx).isLessThan(listIdx);
        }

        @Test
        void elementsAreSeparatedByBlankLines() {
            var layout = new DocumentLayout(List.of(
                    new Title(1, "Title", DUMMY),
                    new Paragraph("Body.", DUMMY)));
            String md = generator.generate(layout);

            // Should have a blank line between title and paragraph
            assertThat(md).contains("# Title\n\nBody.");
        }

        @Test
        void emptyLayoutProducesEmptyString() {
            var layout = new DocumentLayout(List.of());
            String md = generator.generate(layout);
            assertThat(md.trim()).isEmpty();
        }

        @Test
        void complexDocumentWithAllElementTypes() {
            var layout = new DocumentLayout(List.of(
                    new ImageRegion(null, "IMG1", DUMMY),
                    new Title(1, "Vaadin TestBench", DUMMY),
                    new Paragraph("A testing tool.", DUMMY),
                    new Title(2, "Features", DUMMY),
                    new ListBlock(ListBlock.ListType.UNORDERED,
                            List.of("Fast", "Reliable"), DUMMY),
                    new CodeBlock("bash", "mvn test", DUMMY),
                    new Table(List.of("Tool", "Type"),
                            List.of(List.of("JUnit", "Unit")), DUMMY),
                    new Link("https://vaadin.com", "https://vaadin.com", DUMMY)));
            String md = generator.generate(layout);

            assertThat(md).contains("[IMG1]()");
            assertThat(md).contains("# Vaadin TestBench");
            assertThat(md).contains("A testing tool.");
            assertThat(md).contains("## Features");
            assertThat(md).contains("* Fast");
            assertThat(md).contains("```bash");
            assertThat(md).contains("| Tool | Type |");
            assertThat(md).contains("[https://vaadin.com](https://vaadin.com)");
        }
    }

    // --- Helper ---

    private static DocumentLayout layoutOf(DocumentElement element) {
        return new DocumentLayout(List.of(element));
    }
}
