package dev.gushchin.taskmanager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.gushchin.taskmanager.IntegrationTestBase;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailConfigurationIntegrationTest extends IntegrationTestBase {
    @Autowired
    private JavaMailSenderImpl mailSender;

    @Test
    void smtpOperationsShouldHaveTimeouts() {
        Properties properties = mailSender.getJavaMailProperties();

        assertEquals("5000", properties.getProperty("mail.smtp.connectiontimeout"));
        assertEquals("5000", properties.getProperty("mail.smtp.timeout"));
        assertEquals("5000", properties.getProperty("mail.smtp.writetimeout"));
    }
}
