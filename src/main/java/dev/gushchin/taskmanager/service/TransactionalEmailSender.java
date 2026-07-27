package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionalEmailSender {
    private static final String SMTP_AUTH_PROPERTY = "mail.smtp.auth";
    private static final String SMTP_STARTTLS_PROPERTY = "mail.smtp.starttls.enable";

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final MailProperties mailProperties;

    public void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.getMail().getFrom());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            logFailure(ex);
            throw new TransactionalEmailSendingException(ex);
        }
    }

    private void logFailure(MailException exception) {
        Throwable rootCause = getRootCause(exception);
        if (log.isWarnEnabled()) {
            log.warn(
                    "SMTP delivery failed: host={}, port={}, auth={}, startTls={}, usernameConfigured={}, "
                            + "passwordConfigured={}, from={}, errorType={}, errorMessage={}",
                    mailProperties.getHost(),
                    mailProperties.getPort(),
                    mailProperties.getProperties().get(SMTP_AUTH_PROPERTY),
                    mailProperties.getProperties().get(SMTP_STARTTLS_PROPERTY),
                    StringUtils.hasText(mailProperties.getUsername()),
                    StringUtils.hasText(mailProperties.getPassword()),
                    appProperties.getMail().getFrom(),
                    rootCause.getClass().getSimpleName(),
                    rootCause.getMessage());
        }
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && !rootCause.getCause().equals(rootCause)) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}
