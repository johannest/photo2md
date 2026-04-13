package org.vaadin.photo2md.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.photo2md.pipeline.OcrEngine;
import org.vaadin.photo2md.pipeline.ocr.TesseractOcrEngine;

/**
 * Configuration for OCR engine beans, selected via Spring properties.
 * <p>
 * Active engine is determined by {@code photo2md.ocr.engine} property:
 * <ul>
 *   <li>{@code tesseract} — local Tesseract via tess4j (default)</li>
 *   <li>{@code huggingface} — HuggingFace Inference API (Phase 7)</li>
 * </ul>
 */
@Configuration
public class OcrConfiguration {

    @Bean
    @ConditionalOnProperty(name = "photo2md.ocr.engine", havingValue = "tesseract",
            matchIfMissing = true)
    public OcrEngine tesseractOcrEngine(
            @Value("${photo2md.ocr.tesseract.datapath:}") String datapath,
            @Value("${photo2md.ocr.tesseract.language:eng}") String language) {

        // Auto-detect tessdata path if not explicitly configured
        String resolvedDatapath = resolveDatapath(datapath);
        return new TesseractOcrEngine(resolvedDatapath, language);
    }

    private String resolveDatapath(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        // Try common locations
        String[] candidates = {
                "/opt/homebrew/share/tessdata",
                "/opt/homebrew/Cellar/tesseract/5.5.2/share/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/local/share/tessdata",
                "/usr/share/tessdata"
        };

        for (String path : candidates) {
            var dir = java.nio.file.Path.of(path);
            if (java.nio.file.Files.isDirectory(dir)
                    && java.nio.file.Files.exists(dir.resolve("eng.traineddata"))) {
                return path;
            }
        }

        // Fall back to tess4j bundled data (may not have all languages)
        return "";
    }
}
