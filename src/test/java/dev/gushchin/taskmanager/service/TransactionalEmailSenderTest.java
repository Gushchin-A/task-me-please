package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class TransactionalEmailSenderTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AppProperties appProperties = createAppProperties();
    private final TransactionalEmailSender emailSender = new TransactionalEmailSender(mailSender, appProperties);

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
    void sendShouldWrapMailException() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "Message text"));
    }

    private AppProperties createAppProperties() {
        AppProperties properties = new AppProperties();
        properties.getMail().setFrom("no-reply@test.com");

        return properties;
    }
}
