package dev.gushchin.taskmanager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.gushchin.taskmanager.IntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BrevoConfigurationIntegrationTest extends IntegrationTestBase {
    @Autowired
    private AppProperties appProperties;

    @Test
    void brevoOperationsShouldHaveExpectedConfiguration() {
        assertEquals(
                "https://api.brevo.com/v3/smtp/email", appProperties.getBrevo().getApiUrl());
        assertEquals(Duration.ofSeconds(5), appProperties.getBrevo().getConnectTimeout());
        assertEquals(Duration.ofSeconds(5), appProperties.getBrevo().getReadTimeout());
    }
}
