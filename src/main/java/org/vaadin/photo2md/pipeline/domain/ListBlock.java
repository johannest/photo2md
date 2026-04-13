package org.vaadin.photo2md.pipeline.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered or unordered list. Items may contain inline Markdown formatting.
 * Nested lists are represented by embedding a {@link ListBlock} as a child —
 * in the Markdown output the generator handles indentation.
 *
 * @param type     ordered or unordered
 * @param items    list item texts (may include inline Markdown)
 * @param children nested sub-lists, one per parent item index (null entries = no sub-list)
 * @param bounds   position in the original image
 */
public record ListBlock(
        ListType type,
        List<String> items,
        List<ListBlock> children,
        BoundingBox bounds
) implements DocumentElement {

    public enum ListType { ORDERED, UNORDERED }

    /**
     * Convenience constructor without nested children.
     */
    public ListBlock(ListType type, List<String> items, BoundingBox bounds) {
        this(type, items, List.of(), bounds);
    }

    public ListBlock {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("ListBlock must have at least one item");
        }
        if (children == null) {
            children = Collections.emptyList();
        } else {
            children = new ArrayList<>(children); // allow null entries
        }
    }
}
