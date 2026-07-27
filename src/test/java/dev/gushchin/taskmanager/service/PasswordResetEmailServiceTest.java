package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PasswordResetEmailServiceTest {
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);

    @Test
    void sendPasswordResetShouldSendExpectedMessage() {
        AppProperties appProperties = createAppProperties();
        PasswordResetEmailService passwordResetEmailService = new PasswordResetEmailService(emailSender, appProperties);
        User user = new User();
        user.setEmail("user@test.com");

        passwordResetEmailService.sendPasswordReset(user, "reset-token", Duration.ofMinutes(30));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(eq("user@test.com"), eq("Восстановление password в Task Me Please"), textCaptor.capture());
        String text = textCaptor.getValue();

        assertTrue(text.contains("запрос на изменение password"));
        assertTrue(text.contains("https://task-me-please.test/reset-password/reset-token"));
        assertTrue(text.contains("Ссылка действует 30 минут."));
        assertTrue(text.contains("Если вы не запрашивали"));
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test/");

        return appProperties;
    }
}
