package org.vaadin.photo2md.upload.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.photo2md.pipeline.DocumentPipeline;
import org.vaadin.photo2md.pipeline.domain.ProcessingResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Main view: upload a document photo and convert it to Markdown.
 * <p>
 * Flow: upload image → pipeline processes → editor + live preview → download.
 */
@Route("")
@Menu(title = "Upload", order = 0)
@AnonymousAllowed
public class UploadView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(UploadView.class);

    private final DocumentPipeline pipeline;

    private final TextArea markdownEditor;
    private final Markdown markdownPreview;
    private final ProgressBar progressBar;
    private final SplitLayout splitLayout;
    private final Anchor downloadButton;

    public UploadView(DocumentPipeline pipeline) {
        this.pipeline = pipeline;

        var heading = new H1("Photo to Markdown");
        heading.setId("heading");

        // Upload component using modern UploadHandler API
        var upload = createUpload();

        // Progress bar — hidden until processing starts
        progressBar = new ProgressBar();
        progressBar.setId("processing-progress");
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Live Markdown preview (right side of split) — init before editor listener
        markdownPreview = new Markdown("");
        markdownPreview.setId("markdown-preview");
        markdownPreview.setWidthFull();

        // Markdown editor (left side of split)
        markdownEditor = new TextArea();
        markdownEditor.setId("markdown-editor");
        markdownEditor.setWidthFull();
        markdownEditor.setMinHeight("400px");
        markdownEditor.setPlaceholder("Markdown will appear here after processing...");
        markdownEditor.addValueChangeListener(event ->
                markdownPreview.setContent(event.getValue()));

        // Split layout: editor | preview
        var editorWrapper = new VerticalLayout(markdownEditor);
        editorWrapper.setPadding(false);
        editorWrapper.setSizeFull();

        var previewWrapper = new VerticalLayout(markdownPreview);
        previewWrapper.setPadding(false);
        previewWrapper.setSizeFull();

        splitLayout = new SplitLayout(editorWrapper, previewWrapper);
        splitLayout.setSplitterPosition(50);
        splitLayout.setSizeFull();
        splitLayout.setVisible(false);

        // Download button
        downloadButton = createDownloadButton();
        downloadButton.setVisible(false);

        add(heading, upload, progressBar, splitLayout, downloadButton);
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

    private Upload createUpload() {
        var uploadHandler = UploadHandler.inMemory((metadata, data) -> {
            log.info("Received file: {} ({}, {} bytes)",
                    metadata.fileName(), metadata.contentType(), metadata.contentLength());

            UI ui = UI.getCurrent();
            if (ui == null) {
                return;
            }

            ui.access(() -> {
                progressBar.setVisible(true);
                splitLayout.setVisible(false);
                downloadButton.setVisible(false);
            });

            try {
                ProcessingResult result = pipeline.process(data);

                ui.access(() -> {
                    progressBar.setVisible(false);
                    markdownEditor.setValue(result.markdown());
                    markdownPreview.setContent(result.markdown());
                    splitLayout.setVisible(true);
                    downloadButton.setVisible(true);

                    Notification.show(
                            "Processed %d regions in %dms".formatted(
                                    result.layout().elements().size(),
                                    result.processingTime().toMillis()),
                            3000, Notification.Position.BOTTOM_START);
                });
            } catch (Exception e) {
                log.error("Pipeline processing failed", e);
                ui.access(() -> {
                    progressBar.setVisible(false);
                    Notification.show(
                            "Processing failed: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.ERROR);
                });
            }
        });

        var upload = new Upload(uploadHandler);
        upload.setId("upload");
        upload.setAcceptedFileTypes("image/*");
        upload.setMaxFileSize(20 * 1024 * 1024); // 20 MB
        upload.setMaxFiles(1);
        return upload;
    }

    private Anchor createDownloadButton() {
        var downloadHandler = DownloadHandler.fromInputStream(event -> {
            byte[] bytes = markdownEditor.getValue()
                    .getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(
                    new ByteArrayInputStream(bytes),
                    "document.md",
                    "text/markdown",
                    bytes.length);
        });

        var anchor = new Anchor(downloadHandler, "Download Markdown");
        anchor.setId("download-button");

        // Style as a primary button
        var button = new Button("Download Markdown");
        button.addThemeVariants(ButtonVariant.PRIMARY);
        anchor.removeAll();
        anchor.add(button);

        return anchor;
    }
}
