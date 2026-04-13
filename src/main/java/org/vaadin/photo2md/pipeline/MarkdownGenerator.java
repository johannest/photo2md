package org.vaadin.photo2md.pipeline;

import org.vaadin.photo2md.pipeline.domain.DocumentLayout;

/**
 * Converts a fully populated {@link DocumentLayout} (with OCR text)
 * into a Markdown string.
 * <p>
 * Output normalization rules:
 * <ul>
 *   <li>Headings: always {@code #} prefix style (never underline)</li>
 *   <li>Code blocks: always fenced (never indented 4-space)</li>
 *   <li>Links: {@code [text]()} unless text is a URL → {@code [url](url)}</li>
 *   <li>Images: {@code [IMGn]()} with sequential numbering</li>
 * </ul>
 */
public interface MarkdownGenerator {

    /**
     * @param layout the document layout with all text content populated
     * @return the assembled Markdown text
     */
    String generate(DocumentLayout layout);
}
