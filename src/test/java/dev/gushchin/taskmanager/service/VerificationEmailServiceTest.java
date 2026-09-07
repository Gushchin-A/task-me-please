package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

class VerificationEmailServiceTest {
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);
    private final EmailLifetimeFormatter lifetimeFormatter = new EmailLifetimeFormatter();
    private final EmailGreetingFormatter greetingFormatter = new EmailGreetingFormatter();

    @Test
    void sendVerificationShouldSendAgreedHtmlAndText() {
        VerificationEmailService verificationEmailService = createService();
        User user = createUser("Мария");

        verificationEmailService.sendVerification(user, "verification-token", Duration.ofHours(24), "invitation-token");

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("user@test.com"),
                        eq("Подтверждение регистрации в TaskMePlease"),
                        htmlCaptor.capture(),
                        textCaptor.capture());

        String html = htmlCaptor.getValue().replace('\u00A0', ' ');
        assertTrue(html.contains("Подтверждение регистрации"));
        assertTrue(html.contains("Привет, Мария!"));
        assertTrue(html.contains("Чтобы завершить регистрацию в TaskMePlease, необходимо подтвердить почту."));
        assertTrue(html.contains(">Подтвердить почту</a>"));
        assertTrue(
                html.contains("https://task-me-please.test/verify-email/verification-token?invite=invitation-token"));
        assertTrue(html.contains("Ссылка действует 24 часа."));
        assertTrue(html.contains("Если вы не регистрировались в TaskMePlease"));
        assertTrue(html.contains(">TaskMePlease</a>"));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Привет, Мария!"));
        assertTrue(text.contains("Ссылка действует 24 часа."));
        assertTrue(
                text.contains("https://task-me-please.test/verify-email/verification-token?invite=invitation-token"));
    }

    @Test
    void sendVerificationShouldUseNeutralGreetingWhenNameFallsBackToEmail() {
        VerificationEmailService verificationEmailService = createService();
        User user = createUser("user@test.com");

        verificationEmailService.sendVerification(user, "verification-token", Duration.ofHours(24), null);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("user@test.com"),
                        eq("Подтверждение регистрации в TaskMePlease"),
                        htmlCaptor.capture(),
                        ArgumentCaptor.forClass(String.class).capture());

        String html = htmlCaptor.getValue();
        assertTrue(html.contains("Привет!"));
        assertFalse(html.contains("Привет, user@test.com!"));
    }

    @Test
    void sendVerificationShouldUseNeutralGreetingWhenNameIsMissing() {
        VerificationEmailService verificationEmailService = createService();
        User user = createUser(null);

        verificationEmailService.sendVerification(user, "verification-token", Duration.ofHours(1), null);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("user@test.com"),
                        eq("Подтверждение регистрации в TaskMePlease"),
                        htmlCaptor.capture(),
                        ArgumentCaptor.forClass(String.class).capture());

        String html = htmlCaptor.getValue();
        assertTrue(html.contains("Привет!"));
        assertTrue(html.contains("Ссылка действует 1 час."));
    }

    private VerificationEmailService createService() {
        EmailMessageSender messageSender = new EmailMessageSender(
                emailSender,
                new EmailTemplateRenderer(TemplateEngine.createPrecompiled(ContentType.Html)),
                new EmailTextRenderer(),
                createAppProperties());

        return new VerificationEmailService(messageSender, lifetimeFormatter, greetingFormatter);
    }

    private User createUser(String name) {
        User user = new User();
        user.setEmail("user@test.com");
        user.setName(name);

        return user;
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test/");

        return appProperties;
    }
}
