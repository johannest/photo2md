package org.vaadin.photo2md.pipeline.domain;

import java.util.List;

/**
 * Ordered collection of document elements as they appear top-to-bottom
 * in the original image.
 *
 * @param elements the elements in reading order
 */
public record DocumentLayout(List<DocumentElement> elements) {

    public DocumentLayout {
        if (elements == null) {
            elements = List.of();
        }
    }
}
