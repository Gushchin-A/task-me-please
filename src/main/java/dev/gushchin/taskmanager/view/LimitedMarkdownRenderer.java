package dev.gushchin.taskmanager.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

public final class LimitedMarkdownRenderer {
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("_(.+?)_");
    private static final String LINE_BREAK = "<br>";

    private LimitedMarkdownRenderer() {}

    public static String render(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String[] lines = value.split("\\R", -1);
        StringBuilder result = new StringBuilder();
        int lineIndex = 0;

        while (lineIndex < lines.length) {
            if (isQuote(lines[lineIndex])) {
                lineIndex = appendQuote(result, lines, lineIndex);
            } else {
                result.append(renderInline(lines[lineIndex]));
                lineIndex++;
                if (lineIndex < lines.length && !isQuote(lines[lineIndex])) {
                    result.append(LINE_BREAK);
                }
            }
        }

        return result.toString();
    }

    private static int appendQuote(StringBuilder result, String[] lines, int startIndex) {
        result.append("<blockquote>");
        int lineIndex = startIndex;

        while (lineIndex < lines.length && isQuote(lines[lineIndex])) {
            if (lineIndex > startIndex) {
                result.append(LINE_BREAK);
            }
            result.append(renderInline(quoteContent(lines[lineIndex])));
            lineIndex++;
        }

        result.append("</blockquote>");
        return lineIndex;
    }

    private static boolean isQuote(String line) {
        return line.startsWith(">");
    }

    private static String quoteContent(String line) {
        String content = line.substring(1);
        return content.startsWith(" ") ? content.substring(1) : content;
    }

    private static String renderInline(String value) {
        String escapedValue = HtmlUtils.htmlEscape(value);
        String boldValue = replace(BOLD_PATTERN, escapedValue, "<strong>", "</strong>");
        return replace(ITALIC_PATTERN, boldValue, "<em>", "</em>");
    }

    private static String replace(Pattern pattern, String value, String openingTag, String closingTag) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(
                    result, Matcher.quoteReplacement(openingTag + matcher.group(1) + closingTag));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
