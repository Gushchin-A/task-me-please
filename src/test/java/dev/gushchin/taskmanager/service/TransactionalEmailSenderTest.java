package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(OutputCaptureExtension.class)
class TransactionalEmailSenderTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AppProperties appProperties = createAppProperties();
    private final MailProperties mailProperties = createMailProperties();
    private final TransactionalEmailSender emailSender =
            new TransactionalEmailSender(mailSender, appProperties, mailProperties);

    @Test
    void sendShouldBuildExpectedMessage() {
        emailSender.send("user@test.com", "Subject", "Message text");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertEquals("no-reply@test.com", message.getFrom());
        assertEquals("user@test.com", message.getTo()[0]);
        assertEquals("Subject", message.getSubject());
        assertEquals("Message text", message.getText());
    }

    @Test
    void sendShouldWrapMailExceptionAndLogSafeDiagnostics(CapturedOutput output) {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "Message text"));
        assertTrue(output.getAll().contains("SMTP unavailable"));
        assertTrue(output.getAll().contains("host=smtp-relay.brevo.com"));
        assertTrue(output.getAll().contains("port=587"));
        assertTrue(output.getAll().contains("auth=true"));
        assertTrue(output.getAll().contains("startTls=true"));
        assertTrue(output.getAll().contains("usernameConfigured=true"));
        assertTrue(output.getAll().contains("passwordConfigured=true"));
        assertTrue(output.getAll().contains("from=no-reply@test.com"));
        assertTrue(output.getAll().contains("errorType=MailSendException"));
        assertFalse(output.getAll().contains("user@test.com"));
        assertFalse(output.getAll().contains("Message text"));
        assertFalse(output.getAll().contains("smtp-user@test.com"));
        assertFalse(output.getAll().contains("smtp-secret"));
    }

    private AppProperties createAppProperties() {
        AppProperties properties = new AppProperties();
        properties.getMail().setFrom("no-reply@test.com");

        return properties;
    }

    private MailProperties createMailProperties() {
        MailProperties properties = new MailProperties();
        properties.setHost("smtp-relay.brevo.com");
        properties.setPort(587);
        properties.setUsername("smtp-user@test.com");
        properties.setPassword("smtp-secret");
        properties.getProperties().put("mail.smtp.auth", "true");
        properties.getProperties().put("mail.smtp.starttls.enable", "true");

        return properties;
    }
}
