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

class VerificationEmailServiceTest {
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);

    @Test
    void sendVerificationShouldSendExpectedMessage() {
        AppProperties appProperties = createAppProperties();
        VerificationEmailService verificationEmailService = new VerificationEmailService(emailSender, appProperties);
        User user = new User();
        user.setEmail("user@test.com");

        verificationEmailService.sendVerification(user, "verification-token", Duration.ofHours(24), "invitation-token");

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("user@test.com"), eq("Подтвердите email в Task Me Please"), textCaptor.capture());
        String text = textCaptor.getValue();

        assertTrue(text.contains("Добро пожаловать в Task Me Please."));
        assertTrue(
                text.contains("https://task-me-please.test/verify-email/verification-token?invite=invitation-token"));
        assertTrue(text.contains("Ссылка действует 24 часов."));
        assertTrue(text.contains("Если вы не регистрировались"));
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test/");

        return appProperties;
    }
}
