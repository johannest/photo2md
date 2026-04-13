# photo2md Implementation Plan

## Context

Convert mobile phone photos of documents to Markdown text files via a Vaadin 25 PWA. The user wants a **TDD/BDD spec-driven** workflow: write specs and tests first, then implement until they pass. The pipeline must be **modular** (swappable OCR/image processing backends).

## Tech Stack

- **Vaadin 25 Flow** (latest stable) + **Spring Boot 4** (latest stable) + **Java 21**
- **Package**: `org.vaadin.photo2md`
- **Tesseract** via tess4j (default local OCR), **HuggingFace Inference API** (GOT-OCR2.0) as alternative
- **OpenCV** via JavaCV for image preprocessing & segmentation
- **JUnit 5** (unit + integration) + **Playwright** (E2E)
- **Maven** with Surefire (unit) / Failsafe (integration + E2E)
- **Test fixtures**: Real document photos (not programmatically generated)

## Project Structure

```
photo2md/
├── pom.xml
├── src/main/java/org/vaadin/photo2md/
│   ├── Application.java
│   ├── AppShell.java                          # @PWA
│   ├── pipeline/
│   │   ├── domain/                            # Sealed interface + records
│   │   │   ├── DocumentElement.java           # sealed: Title, Paragraph, ListBlock, Table, ImageRegion
│   │   │   ├── BoundingBox.java, Title.java, Paragraph.java, ListBlock.java, Table.java, ImageRegion.java
│   │   │   ├── DocumentLayout.java
│   │   │   └── ProcessingResult.java
│   │   ├── ImagePreprocessor.java             # Interface
│   │   ├── DocumentSegmenter.java             # Interface
│   │   ├── OcrEngine.java                     # Interface
│   │   ├── MarkdownGenerator.java             # Interface
│   │   ├── DocumentPipeline.java              # Orchestrator @Service
│   │   ├── preprocessing/OpenCvImagePreprocessor.java
│   │   ├── segmentation/OpenCvDocumentSegmenter.java
│   │   ├── ocr/TesseractOcrEngine.java
│   │   ├── ocr/HuggingFaceOcrEngine.java
│   │   └── markdown/DefaultMarkdownGenerator.java
│   ├── upload/ui/UploadView.java              # Main route "/"
│   └── config/
│       ├── OcrConfiguration.java              # @ConditionalOnProperty
│       └── OpenCvConfiguration.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-tesseract.properties
│   └── application-huggingface.properties
└── src/test/
    ├── java/org/vaadin/photo2md/
    │   ├── pipeline/
    │   │   ├── MarkdownGeneratorTest.java     # Unit: pure logic, TDD first
    │   │   ├── DocumentElementTest.java       # Unit: domain model
    │   │   ├── DocumentPipelineTest.java      # Unit: mocked orchestration
    │   │   ├── ImagePreprocessorTest.java     # Unit: output properties
    │   │   ├── DocumentSegmenterTest.java     # Unit: element detection
    │   │   ├── OcrEngineTest.java             # Unit: interface contract
    │   │   └── ocr/
    │   │       ├── TesseractOcrEngineIT.java  # Integration: real Tesseract
    │   │       └── HuggingFaceOcrEngineIT.java # Integration: HF API
    │   └── upload/ui/
    │       └── UploadViewPlaywrightIT.java    # E2E: full browser tests
    └── resources/
        ├── specs/                             # BDD spec documents
        │   ├── 01-image-preprocessing.md
        │   ├── 02-document-segmentation.md
        │   ├── 03-ocr-extraction.md
        │   ├── 04-markdown-generation.md
        │   ├── 05-end-to-end-pipeline.md
        │   └── 06-ui-upload-and-preview.md
        └── fixtures/                          # Test images by difficulty
            ├── easy/    (clean scans)
            ├── medium/  (decent phone photos)
            └── hard/    (low-light, skewed, crumpled)
```

## Core Interfaces

```java
public interface ImagePreprocessor {
    byte[] preprocess(byte[] rawImage);  // → grayscale, contrast-enhanced, deskewed
}

public interface DocumentSegmenter {
    DocumentLayout segment(byte[] preprocessedImage);  // → regions with types + bounds
}

public interface OcrEngine {
    String recognize(byte[] imageRegion);  // → extracted text
}

public interface MarkdownGenerator {
    String generate(DocumentLayout layout);  // → assembled Markdown
}
```

Pipeline orchestration: `raw image → preprocess → segment → OCR each region → generate Markdown`

## Domain Model

- `DocumentElement` (sealed interface): `Title(level, text, bounds)`, `Paragraph(text, bounds)`, `ListBlock(type, items, bounds)`, `Table(headers, rows, bounds)`, `ImageRegion(imageData, altText, bounds)`
- `DocumentLayout(List<DocumentElement> elements)` — ordered top-to-bottom
- `ProcessingResult(markdown, layout, processingTime, metadata)`

## UI Design (Vaadin Flow)

Single `UploadView` at route `/`:
1. **Upload** component — accepts `image/*`, 20MB max. On mobile PWA, browser natively offers camera.
2. **ProgressBar** — shown during pipeline processing
3. **SplitLayout** with:
   - Left: **TextArea** (editable Markdown) — id: `markdown-editor`
   - Right: **Markdown** component (live preview) — id: `markdown-preview`
4. **Download button** — `Anchor` + `StreamResource` for `.md` file — id: `download-button`

## Test Strategy

### BDD Specs
Markdown files in `src/test/resources/specs/` using GIVEN/WHEN/THEN format. Human-readable contracts guiding test creation (not auto-parsed).

### Test naming
- `*Test.java` → unit tests (no Spring, fast, Surefire)
- `*IT.java` → integration tests (Spring context, Failsafe)
- `*PlaywrightIT.java` → E2E tests (tagged `@Tag("playwright")`)

### Fixture strategy
- **All levels**: Real document photos (user will provide)
- Easy/medium/hard categories with graduated assertion thresholds
- User provides initial set of real photos; tests use parameterized fixtures with expected text snippets
- Test structure created with placeholder paths; user populates `src/test/resources/fixtures/` before running fixture-dependent tests

### Test runner config
- Surefire excludes `playwright` tag by default
- Failsafe runs all ITs including Playwright
- HuggingFace tests tagged `@Tag("huggingface")`, skippable when no API token

## Implementation Phases

### Phase 1: Project Skeleton + Hello World
- Bootstrap from start.vaadin.com, then customize `pom.xml` with pipeline deps
- `Application.java`, `AppShell.java`
- Minimal `UploadView` with heading + Upload component
- Write + pass `UploadViewPlaywrightIT.pageLoadsWithUploadComponent()`
- Create all 6 spec files (living documentation)
- **Deliverable**: `mvn verify` passes, app runs, Playwright test green

### Phase 2: Domain Model + MarkdownGenerator (pure TDD)
- Define all domain records (sealed interface)
- Write `MarkdownGeneratorTest` first (all element types + mixed doc)
- Implement `DefaultMarkdownGenerator` to pass all tests
- **Deliverable**: Full MarkdownGenerator coverage, `mvn test` green

### Phase 3: Image Preprocessing (OpenCV)
- Write `ImagePreprocessorTest` (grayscale, dimensions, non-null)
- Implement `OpenCvImagePreprocessor` (grayscale, CLAHE contrast, optional deskew)
- **Deliverable**: Preprocessing works on fixture images

### Phase 4: OCR Engine (Tesseract)
- Write `TesseractOcrEngineIT` parameterized over easy fixtures
- Implement `TesseractOcrEngine` via tess4j
- Create `OcrConfiguration` with `@ConditionalOnProperty`
- **Deliverable**: Text extraction from clean images

### Phase 5: Document Segmentation
- Write `DocumentSegmenterTest` (element count + types for easy fixtures)
- Implement `OpenCvDocumentSegmenter` (contour detection, heuristics)
- Start simple: everything as Paragraph, then refine title/list/table detection
- **Deliverable**: At least title vs paragraph distinction for clean scans

### Phase 6: Pipeline Orchestration + UI Integration
- Wire `DocumentPipeline` orchestrator, write mocked unit test + real IT
- Enhance `UploadView`: processing → editor → preview → download
- Full Playwright E2E: upload, verify content, edit, preview, download
- **Deliverable**: Full working app for clean scans

### Phase 7: HuggingFace OCR Backend
- Implement `HuggingFaceOcrEngine` via Spring `RestClient`
- Same parameterized fixtures as Tesseract
- **Deliverable**: Swappable OCR via Spring profile

### Phase 8: Medium/Hard Fixtures + Quality Improvements
- Add real-world photos with graduated assertion thresholds
- Improve preprocessing (noise removal, shadow elimination)
- Refine segmentation heuristics
- **Deliverable**: Reasonable results for real photos

## Verification

After each phase:
1. `mvn test` — all unit tests pass
2. `mvn verify` — all integration + Playwright tests pass
3. Manual: open `http://localhost:8080`, upload a test image, verify result
