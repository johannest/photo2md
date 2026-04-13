package org.vaadin.photo2md.pipeline.domain;

/**
 * A recognized element within a document image.
 * <p>
 * Sealed interface — all permitted subtypes are known at compile time,
 * enabling exhaustive pattern matching with {@code switch}.
 */
public sealed interface DocumentElement
        permits Title, Paragraph, ListBlock, Table, CodeBlock, ImageRegion, Link {

    /** Bounding box of this element in the original image. */
    BoundingBox bounds();
}
