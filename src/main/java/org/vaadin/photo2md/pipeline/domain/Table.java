package org.vaadin.photo2md.pipeline.domain;

import java.util.List;

/**
 * A table with headers and data rows. Cell text may contain inline
 * Markdown formatting and links.
 *
 * @param headers column header texts
 * @param rows    data rows (each row is a list of cell texts)
 * @param bounds  position in the original image
 */
public record Table(
        List<String> headers,
        List<List<String>> rows,
        BoundingBox bounds
) implements DocumentElement {

    public Table {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one header");
        }
        if (rows == null) {
            rows = List.of();
        }
    }
}
