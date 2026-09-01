package dev.gushchin.taskmanager.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LimitedMarkdownRendererTest {
    @Test
    void renderShouldFormatSupportedMarkdown() {
        String markdown = "**Жирный** и _курсив_\n> Первая строка\n> Вторая строка";

        String result = LimitedMarkdownRenderer.render(markdown);

        assertThat(result)
                .isEqualTo("<strong>Жирный</strong> и <em>курсив</em>"
                        + "<blockquote>Первая строка<br>Вторая строка</blockquote>");
    }

    @Test
    void renderShouldEscapeHtml() {
        String result = LimitedMarkdownRenderer.render("<script>alert('xss')</script> **текст**");

        assertThat(result).isEqualTo("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt; <strong>текст</strong>");
    }

    @Test
    void renderShouldPreservePlainTextLineBreaks() {
        assertThat(LimitedMarkdownRenderer.render("Первая строка\nВторая строка"))
                .isEqualTo("Первая строка<br>Вторая строка");
    }
}
