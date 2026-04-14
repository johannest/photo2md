package org.vaadin.photo2md.pipeline.staged;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vaadin.photo2md.pipeline.*;
import org.vaadin.photo2md.pipeline.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link StagedDocumentPipeline} orchestrator.
 * All pipeline stages are mocked — this tests orchestration logic only.
 */
@ExtendWith(MockitoExtension.class)
class StagedDocumentPipelineTest {

    @Mock
    private ImagePreprocessor preprocessor;

    @Mock
    private DocumentSegmenter segmenter;

    @Mock
    private OcrEngine ocrEngine;

    @Mock
    private MarkdownGenerator markdownGenerator;

    @Mock
    private ImageCropper imageCropper;

    private StagedDocumentPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new StagedDocumentPipeline(preprocessor, segmenter, ocrEngine,
                markdownGenerator, imageCropper);
    }

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
    class Orchestration {

        private final byte[] rawImage = {1, 2, 3};
        private final byte[] preprocessed = {4, 5, 6};

        private final byte[] croppedRegion = {10, 11, 12};

        @Test
        void callsStagesInOrder() {
            var bounds = new BoundingBox(0, 0, 100, 20);
            var layout = new DocumentLayout(List.of(new Paragraph("", bounds)));

            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(layout);
            when(imageCropper.crop(eq(preprocessed), any())).thenReturn(croppedRegion);
            when(ocrEngine.recognize(croppedRegion)).thenReturn("Hello world");
            when(markdownGenerator.generate(any())).thenReturn("Hello world\n");

            pipeline.process(rawImage);

            InOrder inOrder = inOrder(preprocessor, segmenter, imageCropper, ocrEngine, markdownGenerator);
            inOrder.verify(preprocessor).preprocess(rawImage);
            inOrder.verify(segmenter).segment(preprocessed);
            inOrder.verify(imageCropper).crop(eq(preprocessed), any());
            inOrder.verify(ocrEngine).recognize(croppedRegion);
            inOrder.verify(markdownGenerator).generate(any());
        }

        @Test
        void ocrIsCalledForEachSegmentedRegion() {
            var bounds1 = new BoundingBox(0, 0, 100, 20);
            var bounds2 = new BoundingBox(0, 30, 100, 20);
            var bounds3 = new BoundingBox(0, 60, 100, 20);
            var layout = new DocumentLayout(List.of(
                    new Paragraph("", bounds1),
                    new Paragraph("", bounds2),
                    new Paragraph("", bounds3)
            ));

            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(layout);
            when(imageCropper.crop(eq(preprocessed), any())).thenReturn(croppedRegion);
            when(ocrEngine.recognize(any())).thenReturn("text");
            when(markdownGenerator.generate(any())).thenReturn("text\n\ntext\n\ntext\n");

            pipeline.process(rawImage);

            verify(ocrEngine, times(3)).recognize(any());
        }

        @Test
        void emptyLayoutProducesEmptyMarkdown() {
            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(new DocumentLayout(List.of()));
            when(markdownGenerator.generate(any())).thenReturn("");

            ProcessingResult result = pipeline.process(rawImage);

            assertThat(result.markdown()).isEmpty();
            verify(ocrEngine, never()).recognize(any());
        }

        @Test
        void skipsRegionWhenCroppingFails() {
            var bounds = new BoundingBox(0, 0, 100, 20);
            var layout = new DocumentLayout(List.of(new Paragraph("", bounds)));

            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(layout);
            when(imageCropper.crop(eq(preprocessed), any())).thenReturn(new byte[0]);
            when(markdownGenerator.generate(any())).thenReturn("");

            pipeline.process(rawImage);

            verify(ocrEngine, never()).recognize(any());
        }
    }

    @Nested
    class ResultProperties {

        private final byte[] rawImage = {1, 2, 3};
        private final byte[] preprocessed = {4, 5, 6};
        private final byte[] croppedRegion = {10, 11, 12};

        @Test
        void resultContainsMarkdown() {
            var layout = new DocumentLayout(List.of(
                    new Paragraph("", new BoundingBox(0, 0, 100, 20))
            ));

            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(layout);
            when(imageCropper.crop(eq(preprocessed), any())).thenReturn(croppedRegion);
            when(ocrEngine.recognize(croppedRegion)).thenReturn("Hello");
            when(markdownGenerator.generate(any())).thenReturn("Hello\n");

            ProcessingResult result = pipeline.process(rawImage);

            assertThat(result.markdown()).isEqualTo("Hello\n");
        }

        @Test
        void resultContainsLayout() {
            var bounds = new BoundingBox(0, 0, 100, 20);
            var layout = new DocumentLayout(List.of(new Paragraph("", bounds)));

            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(layout);
            when(imageCropper.crop(eq(preprocessed), any())).thenReturn(croppedRegion);
            when(ocrEngine.recognize(croppedRegion)).thenReturn("Hello");
            when(markdownGenerator.generate(any())).thenReturn("Hello\n");

            ProcessingResult result = pipeline.process(rawImage);

            assertThat(result.layout()).isNotNull();
            assertThat(result.layout().elements()).hasSize(1);
        }

        @Test
        void resultTracksProcessingTime() {
            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(new DocumentLayout(List.of()));
            when(markdownGenerator.generate(any())).thenReturn("");

            ProcessingResult result = pipeline.process(rawImage);

            assertThat(result.processingTime()).isNotNull();
            assertThat(result.processingTime().toMillis()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void resultContainsMetadata() {
            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(new DocumentLayout(List.of()));
            when(markdownGenerator.generate(any())).thenReturn("");

            ProcessingResult result = pipeline.process(rawImage);

            assertThat(result.metadata()).isNotNull();
            assertThat(result.metadata()).containsKey("regions");
        }

        @Test
        void ocrTextIsPopulatedIntoElements() {
            var bounds = new BoundingBox(0, 0, 100, 20);
            var layout = new DocumentLayout(List.of(new Paragraph("", bounds)));

            when(preprocessor.preprocess(rawImage)).thenReturn(preprocessed);
            when(segmenter.segment(preprocessed)).thenReturn(layout);
            when(imageCropper.crop(eq(preprocessed), any())).thenReturn(croppedRegion);
            when(ocrEngine.recognize(croppedRegion)).thenReturn("Recognized text");
            when(markdownGenerator.generate(any())).thenAnswer(invocation -> {
                DocumentLayout passedLayout = invocation.getArgument(0);
                var elements = passedLayout.elements();
                assertThat(elements).hasSize(1);
                assertThat(elements.getFirst()).isInstanceOf(Paragraph.class);
                assertThat(((Paragraph) elements.getFirst()).text()).isEqualTo("Recognized text");
                return "Recognized text\n";
            });

            pipeline.process(rawImage);

            verify(markdownGenerator).generate(any());
        }
    }
}
