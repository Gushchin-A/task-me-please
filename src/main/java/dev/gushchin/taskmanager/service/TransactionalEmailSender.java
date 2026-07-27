package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionalEmailSender {
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.getMail().getFrom());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new TransactionalEmailSendingException(ex);
        }
    }
}
