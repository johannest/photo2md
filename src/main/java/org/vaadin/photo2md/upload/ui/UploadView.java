package org.vaadin.photo2md.upload.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Main view: upload a document photo and convert it to Markdown.
 * <p>
 * Phase 1: minimal skeleton with upload component only.
 * Pipeline integration, editor, preview, and download come in Phase 6.
 */
@Route("")
@Menu(title = "Upload", order = 0)
@AnonymousAllowed
public class UploadView extends VerticalLayout {

    public UploadView() {
        var heading = new H1("Photo to Markdown");
        heading.setId("heading");

        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setId("upload");
        upload.setAcceptedFileTypes("image/*");
        upload.setMaxFileSize(20 * 1024 * 1024); // 20 MB

        add(heading, upload);
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }
}
