package org.vaadin.photo2md.pipeline.vision;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VisionLlmPipeline} abstract base class.
 * Uses a concrete stub subclass to test shared logic.
 */
class VisionLlmPipelineTest {

    private static final String CANNED_MARKDOWN = "# Hello World\n\nSome paragraph text.";

    private final StubVisionPipeline pipeline = new StubVisionPipeline(CANNED_MARKDOWN);

    @Nested
    class Validation {

        @Test
        void rejectsNullInput() {
            assertThatThrownBy(() -> pipeline.process(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        void rejectsEmptyInput() {
            assertThatThrownBy(() -> pipeline.process(new byte[0]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }
    }

    @Nested
    class ResultProperties {

        @Test
        void returnsMarkdownFromSubclass() {
            ProcessingResult result = pipeline.process(new byte[]{1, 2, 3});

            assertThat(result.markdown()).isEqualTo(CANNED_MARKDOWN);
        }

        @Test
        void returnsEmptyLayout() {
            ProcessingResult result = pipeline.process(new byte[]{1, 2, 3});

            assertThat(result.layout()).isNotNull();
            assertThat(result.layout().elements()).isEmpty();
        }

        @Test
        void tracksProcessingTime() {
            ProcessingResult result = pipeline.process(new byte[]{1, 2, 3});

            assertThat(result.processingTime()).isNotNull();
            assertThat(result.processingTime().toMillis()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void metadataContainsBackendName() {
            ProcessingResult result = pipeline.process(new byte[]{1, 2, 3});

            assertThat(result.metadata()).containsEntry("backend", "stub");
        }

        @Test
        void metadataContainsProcessingTime() {
            ProcessingResult result = pipeline.process(new byte[]{1, 2, 3});

            assertThat(result.metadata()).containsKey("processingTimeMs");
        }
    }

    @Nested
    class ImageEncoding {

        @Test
        void passesBase64EncodedImage() {
            pipeline.process(new byte[]{1, 2, 3});

            assertThat(pipeline.lastBase64Image).isNotNull();
            assertThat(pipeline.lastBase64Image).isEqualTo("AQID"); // base64 of {1,2,3}
        }

        @Test
        void detectsPngMediaType() {
            // PNG magic bytes: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
            byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            pipeline.process(png);

            assertThat(pipeline.lastMediaType).isEqualTo("image/png");
        }

        @Test
        void detectsJpegMediaType() {
            // JPEG magic bytes: 0xFF 0xD8 0xFF
            byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
            pipeline.process(jpeg);

            assertThat(pipeline.lastMediaType).isEqualTo("image/jpeg");
        }

        @Test
        void defaultsToPngForUnknownFormat() {
            pipeline.process(new byte[]{1, 2, 3});

            assertThat(pipeline.lastMediaType).isEqualTo("image/png");
        }
    }

    @Nested
    class MarkdownFenceStripping {

        @Test
        void stripsMarkdownFence() {
            var fenced = new StubVisionPipeline("```markdown\n# Title\n\nText\n```");
            ProcessingResult result = fenced.process(new byte[]{1, 2, 3});

            assertThat(result.markdown()).isEqualTo("# Title\n\nText");
        }

        @Test
        void stripsMdFence() {
            var fenced = new StubVisionPipeline("```md\n# Title\n```");
            ProcessingResult result = fenced.process(new byte[]{1, 2, 3});

            assertThat(result.markdown()).isEqualTo("# Title");
        }

        @Test
        void stripsGenericFence() {
            var fenced = new StubVisionPipeline("```\n# Title\n```");
            ProcessingResult result = fenced.process(new byte[]{1, 2, 3});

            assertThat(result.markdown()).isEqualTo("# Title");
        }

        @Test
        void doesNotStripNonFencedContent() {
            var plain = new StubVisionPipeline("# Title\n\nText");
            ProcessingResult result = plain.process(new byte[]{1, 2, 3});

            assertThat(result.markdown()).isEqualTo("# Title\n\nText");
        }

        @Test
        void handlesNullResponse() {
            var nullResponse = new StubVisionPipeline(null);
            ProcessingResult result = nullResponse.process(new byte[]{1, 2, 3});

            assertThat(result.markdown()).isEmpty();
        }
    }

    /**
     * Concrete stub for testing the abstract base class.
     */
    private static class StubVisionPipeline extends VisionLlmPipeline {

        private final String cannedResponse;
        String lastBase64Image;
        String lastMediaType;

        StubVisionPipeline(String cannedResponse) {
            this.cannedResponse = cannedResponse;
        }

        @Override
        protected String callVisionApi(String base64Image, String mediaType) {
            this.lastBase64Image = base64Image;
            this.lastMediaType = mediaType;
            return cannedResponse;
        }

        @Override
        protected String backendName() {
            return "stub";
        }
    }
}
