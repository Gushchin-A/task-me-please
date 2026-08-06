package dev.gushchin.taskmanager.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private String baseUrl;
    private Brevo brevo = new Brevo();
    private Mail mail = new Mail();
    private Security security = new Security();

    @Getter
    @Setter
    public static class Brevo {
        private String apiKey;
        private String apiUrl;
        private Duration connectTimeout;
        private Duration readTimeout;
    }

    @Getter
    @Setter
    public static class Mail {
        private String from;
    }

    @Getter
    @Setter
    public static class Security {
        private String rememberMeKey;
    }
}
