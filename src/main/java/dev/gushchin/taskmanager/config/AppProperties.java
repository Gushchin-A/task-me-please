package dev.gushchin.taskmanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private String baseUrl;
    private Mail mail = new Mail();
    private Security security = new Security();

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
