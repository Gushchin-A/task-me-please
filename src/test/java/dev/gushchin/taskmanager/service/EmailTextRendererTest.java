package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.gushchin.taskmanager.view.EmailActionView;
import dev.gushchin.taskmanager.view.EmailContentView;
import dev.gushchin.taskmanager.view.EmailParameterView;
import org.junit.jupiter.api.Test;

class EmailTextRendererTest {
    private final EmailTextRenderer textRenderer = new EmailTextRenderer();

    @Test
    void renderShouldKeepBodyParametersActionAndNotesInOrder() {
        EmailContentView content = EmailContentView.builder()
                .heading("Задача обновлена")
                .bodyParagraph("Андрей изменил исполнителя задачи.")
                .parameter(new EmailParameterView("Задача", "Подготовить релиз на Render"))
                .parameter(new EmailParameterView("Команда", "Креативный заводиксвс"))
                .action(new EmailActionView("Открыть задачу", "https://task-me-please.test/tasks/94"))
                .note("Ссылка действует 24 часа.")
                .note("Если вы не регистрировались, проигнорируйте письмо.")
                .build();

        String text = textRenderer.render(content);

        assertEquals(
                """
                Андрей изменил исполнителя задачи.

                Задача: Подготовить релиз на Render
                Команда: Креативный заводиксвс

                Открыть задачу: https://task-me-please.test/tasks/94

                Ссылка действует 24 часа.
                Если вы не регистрировались, проигнорируйте письмо.""",
                text);
    }

    @Test
    void renderShouldSkipEmptyBlocks() {
        EmailContentView content = EmailContentView.builder()
                .heading("Команда удалена")
                .bodyParagraph("Команда «Креативный заводиксвс» была удалена владельцем. "
                        + "У вас больше нет доступа к задачам и данным этой команды.")
                .build();

        String text = textRenderer.render(content);

        assertEquals(
                "Команда «Креативный заводиксвс» была удалена владельцем. "
                        + "У вас больше нет доступа к задачам и данным этой команды.",
                text);
    }
}
