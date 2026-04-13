# Markdown Generation Specification

## Title Elements
- GIVEN a Title with level 1 and text "Hello World"
- WHEN the markdown generator processes it
- THEN the output contains "# Hello World"

- GIVEN a Title with level 3 and text "Subsection"
- WHEN the markdown generator processes it
- THEN the output contains "### Subsection"

- NOTE: Always use # prefix style (never ====== underline style)

## Paragraph Elements
- GIVEN a Paragraph with text "Lorem ipsum dolor sit amet."
- WHEN the markdown generator processes it
- THEN the output contains the text as-is with blank lines around it

## Paragraph with Inline Styles
- GIVEN a Paragraph with bold text "**important**" within it
- THEN the bold markers are preserved in the output

- GIVEN a Paragraph with italic text "*emphasis*" within it
- THEN the italic markers are preserved in the output

- GIVEN a Paragraph with inline code "`variable`" within it
- THEN the backtick markers are preserved in the output

## Ordered List
- GIVEN a ListBlock of type ORDERED with items ["First", "Second", "Third"]
- WHEN the markdown generator processes it
- THEN the output contains:
  1. First
  2. Second
  3. Third

## Unordered List
- GIVEN a ListBlock of type UNORDERED with items ["Alpha", "Beta"]
- WHEN the markdown generator processes it
- THEN the output contains:
  * Alpha
  * Beta

## Nested Lists
- GIVEN an ordered ListBlock where item 2 has a nested unordered ListBlock
- THEN the nested items are indented with 3 spaces and use the sub-list marker

## Table
- GIVEN a Table with headers ["Name", "Age"] and rows [["Alice", "30"], ["Bob", "25"]]
- WHEN the markdown generator processes it
- THEN the output contains a pipe-delimited markdown table with header separator:
  | Name  | Age |
  |-------|-----|
  | Alice | 30  |
  | Bob   | 25  |

## Table with Inline Links
- GIVEN a Table where a cell contains a link "[Platform Version]()"
- THEN the link syntax is preserved within the table cell

## Code Blocks
- GIVEN a CodeBlock with language "java" and code content
- WHEN the markdown generator processes it
- THEN the output is a fenced code block:
  ```java
  <code content here>
  ```

- GIVEN a CodeBlock with no language specified
- THEN the output uses a bare fenced block: ``` ... ```

- NOTE: Always use fenced style (never indented 4-space style)

## Image Placeholders
- GIVEN an ImageRegion with altText "IMG1"
- WHEN the markdown generator processes it
- THEN the output contains "[IMG1]()"

- GIVEN multiple ImageRegions in a document
- THEN they are numbered sequentially: [IMG1](), [IMG2](), etc.

## Links
- GIVEN a Link with text "the documentation" (not a URL)
- THEN the output is "[the documentation]()" — empty URL

- GIVEN a Link with text "https://example.com" (text IS a URL)
- THEN the output is "[https://example.com](https://example.com)" — URL preserved

## Mixed Document
- GIVEN a DocumentLayout with [Title(1, "Report"), Paragraph("Intro."), ListBlock(ORDERED, ["A","B"])]
- WHEN the markdown generator processes it
- THEN the output has elements in order, separated by blank lines
- AND the result is valid Markdown
