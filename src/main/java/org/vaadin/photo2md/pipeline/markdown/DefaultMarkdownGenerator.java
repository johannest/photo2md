package org.vaadin.photo2md.pipeline.markdown;

import org.springframework.stereotype.Component;
import org.vaadin.photo2md.pipeline.MarkdownGenerator;
import org.vaadin.photo2md.pipeline.domain.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation that converts a {@link DocumentLayout} to Markdown text.
 * <p>
 * Normalization rules applied:
 * <ul>
 *   <li>Headings: always {@code #} prefix style</li>
 *   <li>Code blocks: always fenced ({@code ```})</li>
 *   <li>Links: {@code [text]()} unless text looks like a URL</li>
 *   <li>Images: {@code [IMGn]()} with sequential numbering</li>
 * </ul>
 */
@Component
public class DefaultMarkdownGenerator implements MarkdownGenerator {

    @Override
    public String generate(DocumentLayout layout) {
        if (layout == null || layout.elements().isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        var imageCounter = new AtomicInteger(0);
        var elements = layout.elements();

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append("\n\n");
            }
            renderElement(elements.get(i), sb, imageCounter, "");
        }

        sb.append('\n');
        return sb.toString();
    }

    private void renderElement(DocumentElement element, StringBuilder sb,
                               AtomicInteger imageCounter, String indent) {
        switch (element) {
            case Title t -> renderTitle(t, sb);
            case Paragraph p -> sb.append(indent).append(p.text());
            case ListBlock l -> renderList(l, sb, imageCounter, indent);
            case Table t -> renderTable(t, sb);
            case CodeBlock c -> renderCodeBlock(c, sb);
            case ImageRegion img -> renderImage(img, sb, imageCounter);
            case Link link -> renderLink(link, sb);
        }
    }

    private void renderTitle(Title title, StringBuilder sb) {
        sb.append("#".repeat(title.level())).append(' ').append(title.text());
    }

    private void renderList(ListBlock list, StringBuilder sb,
                            AtomicInteger imageCounter, String indent) {
        var items = list.items();
        var children = list.children();

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(indent);
            if (list.type() == ListBlock.ListType.ORDERED) {
                sb.append(i + 1).append(". ");
            } else {
                sb.append("* ");
            }
            sb.append(items.get(i));

            // Render nested child list if present
            if (i < children.size() && children.get(i) != null) {
                sb.append('\n');
                renderList(children.get(i), sb, imageCounter, indent + "   ");
            }
        }
    }

    private void renderTable(Table table, StringBuilder sb) {
        var headers = table.headers();

        // Header row
        sb.append("| ");
        sb.append(String.join(" | ", headers));
        sb.append(" |");

        // Separator row
        sb.append('\n');
        sb.append("|");
        for (int i = 0; i < headers.size(); i++) {
            sb.append("---|");
        }

        // Data rows
        for (List<String> row : table.rows()) {
            sb.append('\n');
            sb.append("| ");
            sb.append(String.join(" | ", row));
            sb.append(" |");
        }
    }

    private void renderCodeBlock(CodeBlock code, StringBuilder sb) {
        sb.append("```");
        if (!code.language().isEmpty()) {
            sb.append(code.language());
        }
        sb.append('\n');
        sb.append(code.code());
        sb.append('\n');
        sb.append("```");
    }

    private void renderImage(ImageRegion img, StringBuilder sb, AtomicInteger counter) {
        String alt = img.altText();
        // Auto-number images with default "IMG" alt text
        if ("IMG".equals(alt)) {
            alt = "IMG" + counter.incrementAndGet();
        } else if (alt.matches("IMG\\d+")) {
            // Already numbered — use as-is but still track the count
            counter.incrementAndGet();
        }
        sb.append('[').append(alt).append("]()");
    }

    private void renderLink(Link link, StringBuilder sb) {
        String text = link.text();
        String url = link.url();

        // If text looks like a URL, use it as the URL too
        if (looksLikeUrl(text)) {
            url = text;
        }

        sb.append('[').append(text).append("](").append(url).append(')');
    }

    private static boolean looksLikeUrl(String text) {
        return text != null
                && (text.startsWith("http://") || text.startsWith("https://"));
    }
}
