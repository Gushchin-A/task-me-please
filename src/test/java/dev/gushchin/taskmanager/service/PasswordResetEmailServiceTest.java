package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.User;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PasswordResetEmailServiceTest {
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);
    private final EmailLifetimeFormatter lifetimeFormatter = new EmailLifetimeFormatter();
    private final EmailGreetingFormatter greetingFormatter = new EmailGreetingFormatter();

    @Test
    void sendPasswordResetShouldSendAgreedHtmlAndText() {
        EmailMessageSender messageSender = new EmailMessageSender(
                emailSender,
                new EmailTemplateRenderer(TemplateEngine.createPrecompiled(ContentType.Html)),
                new EmailTextRenderer(),
                createAppProperties());
        PasswordResetEmailService passwordResetEmailService =
                new PasswordResetEmailService(messageSender, lifetimeFormatter, greetingFormatter);
        User user = new User();
        user.setEmail("user@test.com");
        user.setName("Мария");

        passwordResetEmailService.sendPasswordReset(user, "reset-token", Duration.ofMinutes(30));

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("user@test.com"),
                        eq("Восстановление пароля в TaskMePlease"),
                        htmlCaptor.capture(),
                        textCaptor.capture());

        String html = htmlCaptor.getValue();
        assertTrue(html.contains("Восстановление пароля"));
        assertTrue(html.contains("Привет, Мария!"));
        assertTrue(html.contains("Мы получили запрос на изменение пароля вашего аккаунта."));
        assertTrue(html.contains("Установить новый пароль"));
        assertTrue(html.contains("https://task-me-please.test/reset-password/reset-token"));
        assertTrue(html.contains("Ссылка действует 30 минут."));
        assertTrue(html.contains("Если вы не запрашивали изменение пароля"));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Мы получили запрос на изменение пароля вашего аккаунта."));
        assertTrue(text.contains("https://task-me-please.test/reset-password/reset-token"));
        assertTrue(text.contains("Ссылка действует 30 минут."));
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test/");

        return appProperties;
    }
}
