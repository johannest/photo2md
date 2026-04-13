# End-to-End Pipeline Specification

## Full Pipeline Flow
- GIVEN a raw document photograph
- WHEN the pipeline processes it
- THEN it returns a ProcessingResult containing:
  - Valid Markdown text
  - A DocumentLayout with identified elements
  - Processing time > 0
  - Metadata map (at minimum: OCR engine name)

## Pipeline Orchestration Order
- The pipeline MUST execute steps in this order:
  1. ImagePreprocessor.preprocess(rawImage)
  2. DocumentSegmenter.segment(preprocessedImage)
  3. OcrEngine.recognize(region) — for each text region
  4. MarkdownGenerator.generate(populatedLayout)

## Easy Fixture: titles-list
- GIVEN the titles-list.png fixture
- WHEN the pipeline processes it
- THEN the output Markdown contains:
  - "# photo2md" as a level-1 heading
  - "# Architecture" as a level-1 heading
  - "# Planned features" as a level-1 heading
  - An unordered list with "Vaadin 25 PWA web application"
  - An ordered list with "Upload form"

## Easy Fixture: code-blocks-1
- GIVEN the code-blocks-1.png fixture
- WHEN the pipeline processes it
- THEN the output Markdown contains:
  - A heading "Simple type-safe server communication"
  - At least one fenced code block with recognizable code

## Easy Fixture: title-links-table
- GIVEN the title-links-table.png fixture
- WHEN the pipeline processes it
- THEN the output Markdown contains:
  - A heading with "Vaadin Flow"
  - A markdown table with "Branch" column
  - At least one link syntax [text]()

## Easy Fixture: img-titles-paragraphs-link-code
- GIVEN the img-titles-paragraphs-link-code.png fixture
- WHEN the pipeline processes it
- THEN the output Markdown contains:
  - An image placeholder [IMG1]()
  - A heading "Vaadin TestBench"
  - A heading "Releases"
  - A code block with "git clone"

## Error Handling
- GIVEN a corrupted or unreadable image
- WHEN the pipeline processes it
- THEN it throws a meaningful exception (not a generic NPE)
