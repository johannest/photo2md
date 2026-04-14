# photo2md

Convert photos of documents to Markdown text files. Upload a document photo from your mobile phone or desktop and get clean, structured Markdown output.

## Architecture

- **Vaadin 25 Flow** PWA web application (server-side Java UI)
- **Spring Boot 4** with profile-based backend selection
- **Java 21** (records, sealed interfaces, pattern matching)

## Pipeline Modes

photo2md supports three processing backends, selected via the `photo2md.pipeline.mode` property or Spring profiles:

| Mode | Profile | Description | Quality | Privacy |
|------|---------|-------------|---------|---------|
| `staged` | (default) | Classical pipeline: OpenCV preprocessing, segmentation, Tesseract OCR, Markdown generation | Medium | Full — no data leaves your machine |
| `claude` | `claude` | Single-shot Claude Vision API call | Highest | Cloud — image sent to Anthropic API |
| `local-llm` | `local-llm` | Single-shot local LLM via OpenAI-compatible API (LM Studio, Ollama) | Good | Full — no data leaves your machine |

### Running each mode

```bash
# Default (Tesseract — no API key needed)
mvn spring-boot:run

# Claude API (highest quality)
ANTHROPIC_API_KEY=sk-... mvn spring-boot:run -Dspring.profiles.active=claude

# Local LLM (requires a running OpenAI-compatible server, e.g. LM Studio)
mvn spring-boot:run -Dspring.profiles.active=local-llm
```

## Features

1. Upload form for taking photos with mobile phone camera or selecting files
2. Server-side photo-to-Markdown pipeline with swappable backends
3. Live Markdown editor with split-pane preview
4. Download generated Markdown files
5. Extensive test suite:
   - Unit tests for each pipeline stage and element type
   - Integration tests with fixture images (easy/medium/hard)
   - Playwright E2E browser tests

## Build & Test

```bash
mvn spring-boot:run                              # Run app (dev mode)
mvn test                                         # Unit tests only
mvn verify -DexcludedGroups="playwright"          # Unit + integration tests
mvn verify                                       # All tests including Playwright E2E
```

## Configuration

Key properties (set in `application.properties` or via environment variables):

| Property | Default | Description |
|----------|---------|-------------|
| `photo2md.pipeline.mode` | `staged` | Pipeline backend: `staged`, `claude`, or `local-llm` |
| `photo2md.ocr.engine` | `tesseract` | OCR engine for staged mode |
| `photo2md.claude.api-key` | — | Anthropic API key (required for `claude` mode) |
| `photo2md.claude.model` | `claude-sonnet-4-20250514` | Claude model to use |
| `photo2md.local-llm.base-url` | `http://localhost:1234` | Local LLM server URL |
| `photo2md.local-llm.model` | `qwen2.5-vl` | Local vision model name |
