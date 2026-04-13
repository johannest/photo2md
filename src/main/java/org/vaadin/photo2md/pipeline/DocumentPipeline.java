package org.vaadin.photo2md.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.vaadin.photo2md.pipeline.domain.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full document-to-Markdown pipeline:
 * <ol>
 *   <li>Preprocess the raw image</li>
 *   <li>Segment into regions</li>
 *   <li>Crop and OCR each region</li>
 *   <li>Build populated layout and generate Markdown</li>
 * </ol>
 */
@Service
public class DocumentPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentPipeline.class);

    private final ImagePreprocessor preprocessor;
    private final DocumentSegmenter segmenter;
    private final OcrEngine ocrEngine;
    private final MarkdownGenerator markdownGenerator;
    private final ImageCropper imageCropper;

    public DocumentPipeline(ImagePreprocessor preprocessor,
                            DocumentSegmenter segmenter,
                            OcrEngine ocrEngine,
                            MarkdownGenerator markdownGenerator,
                            ImageCropper imageCropper) {
        this.preprocessor = preprocessor;
        this.segmenter = segmenter;
        this.ocrEngine = ocrEngine;
        this.markdownGenerator = markdownGenerator;
        this.imageCropper = imageCropper;
    }

    /**
     * Processes a raw document image through the full pipeline.
     *
     * @param rawImage the uploaded image bytes (PNG or JPEG)
     * @return the processing result containing Markdown, layout, timing, and metadata
     * @throws IllegalArgumentException if the input is null or empty
     */
    public ProcessingResult process(byte[] rawImage) {
        validateInput(rawImage);

        Instant start = Instant.now();

        // Stage 1: Preprocess
        log.info("Pipeline: preprocessing image ({} bytes)", rawImage.length);
        byte[] preprocessed = preprocessor.preprocess(rawImage);

        // Stage 2: Segment
        log.info("Pipeline: segmenting preprocessed image ({} bytes)", preprocessed.length);
        DocumentLayout rawLayout = segmenter.segment(preprocessed);
        log.info("Pipeline: found {} regions", rawLayout.elements().size());

        // Stage 3: OCR each region
        DocumentLayout populatedLayout = ocrRegions(preprocessed, rawLayout);

        // Stage 4: Generate Markdown
        String markdown = markdownGenerator.generate(populatedLayout);

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Pipeline: completed in {}ms, {} characters of Markdown",
                elapsed.toMillis(), markdown.length());

        return new ProcessingResult(
                markdown,
                populatedLayout,
                elapsed,
                Map.of(
                        "regions", String.valueOf(rawLayout.elements().size()),
                        "processingTimeMs", String.valueOf(elapsed.toMillis())
                )
        );
    }

    private void validateInput(byte[] rawImage) {
        if (rawImage == null) {
            throw new IllegalArgumentException("Input image must not be null");
        }
        if (rawImage.length == 0) {
            throw new IllegalArgumentException("Input image must not be empty");
        }
    }

    /**
     * Crops each region from the preprocessed image and runs OCR,
     * then builds a new layout with text-populated elements.
     */
    private DocumentLayout ocrRegions(byte[] preprocessedImage, DocumentLayout rawLayout) {
        if (rawLayout.elements().isEmpty()) {
            return rawLayout;
        }

        List<DocumentElement> populated = new ArrayList<>();

        for (DocumentElement element : rawLayout.elements()) {
            BoundingBox bounds = element.bounds();
            byte[] regionImage = imageCropper.crop(preprocessedImage, bounds);

            if (regionImage.length == 0) {
                log.warn("Failed to crop region at {}, skipping", bounds);
                continue;
            }

            String text = ocrEngine.recognize(regionImage);
            populated.add(populateElement(element, text));
        }

        return new DocumentLayout(populated);
    }

    /**
     * Creates a new element with OCR text populated.
     * Currently all segmented regions are Paragraphs; this will handle
     * other types when Phase 8 adds classification.
     */
    private DocumentElement populateElement(DocumentElement element, String text) {
        return switch (element) {
            case Paragraph p -> new Paragraph(text, p.bounds());
            case Title t -> new Title(t.level(), text.isBlank() ? t.text() : text, t.bounds());
            case CodeBlock c -> new CodeBlock(c.language(), text, c.bounds());
            case ListBlock l -> l; // Lists retain their structure
            case Table t -> t;     // Tables retain their structure
            case ImageRegion i -> i; // Images don't need OCR text
            case Link link -> link;  // Links retain their text
        };
    }
}
