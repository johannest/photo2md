package org.vaadin.photo2md.pipeline.ocr;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.photo2md.pipeline.OcrEngine;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixReadMem;

/**
 * OCR engine backed by Tesseract via JavaCV/bytedeco bindings.
 * Uses the bundled native Tesseract library (no system installation needed).
 */
public class TesseractOcrEngine implements OcrEngine {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrEngine.class);

    private final String datapath;
    private final String language;

    public TesseractOcrEngine(String datapath, String language) {
        this.datapath = datapath;
        this.language = language;
        log.info("Initialized TesseractOcrEngine: datapath={}, language={}", datapath, language);
    }

    @Override
    public String recognize(byte[] imageRegion) {
        if (imageRegion == null || imageRegion.length == 0) {
            throw new IllegalArgumentException("Image region must not be null or empty");
        }

        try (TessBaseAPI api = new TessBaseAPI()) {
            int initResult = api.Init(datapath, language);
            if (initResult != 0) {
                throw new RuntimeException(
                        "Failed to initialize Tesseract with datapath=%s, language=%s"
                                .formatted(datapath, language));
            }

            // Page segmentation mode 3 = fully automatic
            api.SetPageSegMode(3);

            PIX pix = pixReadMem(imageRegion, imageRegion.length);
            if (pix == null) {
                throw new IllegalArgumentException(
                        "Unable to decode image — invalid or unsupported format");
            }

            try {
                api.SetImage(pix);
                BytePointer resultPtr = api.GetUTF8Text();
                if (resultPtr == null) {
                    return "";
                }

                try {
                    String text = resultPtr.getString().trim();
                    log.debug("OCR result: {} characters extracted", text.length());
                    return text;
                } finally {
                    resultPtr.deallocate();
                }
            } finally {
                pixDestroy(pix);
            }
        }
    }
}
