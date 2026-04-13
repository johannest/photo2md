# photo2md

Photo-to-Markdown converter: Vaadin 25.1.1 Flow PWA + Spring Boot 4 + Java 21.

## Project Overview

Package: `org.vaadin.photo2md`
Build: Maven (single module)
See `PLAN.md` for the full implementation plan and architecture.

## Tech Stack

- **Vaadin 25 Flow** (server-side Java UI, no Hilla/React)
- **Spring Boot 4** with Spring profiles for backend switching
- **Java 21** (use records, sealed interfaces, pattern matching)
- **Tesseract** (tess4j) / **HuggingFace Inference API** for OCR
- **OpenCV** (JavaCV) for image preprocessing and segmentation

## Architecture

Modular pipeline with swappable backends via interfaces:
- `ImagePreprocessor` / `DocumentSegmenter` / `OcrEngine` / `MarkdownGenerator`
- Implementations selected via `@ConditionalOnProperty` and Spring profiles
- Orchestrated by `DocumentPipeline` service

## Code Style

- Use Java 21 features: records, sealed interfaces, pattern matching, text blocks
- Prefer constructor injection (no `@Autowired` on fields)
- Domain objects are records in `pipeline.domain` package
- Vaadin views go in feature-based packages (e.g., `upload.ui`)
- Configuration classes in `config` package
- No Lombok -- use records instead

## Build & Run

```bash
mvn spring-boot:run                              # Run app (dev mode)
mvn test                                         # Unit tests only
mvn verify                                       # Unit + integration + Playwright
mvn verify -DexcludedGroups="playwright"          # Skip Playwright E2E
mvn verify -Dspring.profiles.active=huggingface   # Use HuggingFace OCR
```

## SonarQube

Run analysis with: `mvn sonar:sonar`
All code must pass SonarQube quality gate before merging. See `.claude/rules/testing.md` for thresholds.

## Key Conventions

- Every pipeline interface implementation must be a Spring `@Component` or `@Bean`
- Component IDs for testability: `upload`, `processing-progress`, `markdown-editor`, `markdown-preview`, `download-button`
- Test fixtures in `src/test/resources/fixtures/{easy,medium,hard}/` (each has `.png` + expected `.md`)
- BDD specs in `src/test/resources/specs/`
- Output normalization: always `#` headings (not underline), always fenced code blocks (not indented)
- Links: `[text]()` when URL unknown, `[url](url)` when link text is itself a URL
- Images: `[IMGn]()` placeholders with sequential numbering
