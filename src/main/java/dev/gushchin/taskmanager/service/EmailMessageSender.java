package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.view.EmailContentView;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailMessageSender {
    private static final String LAYOUT = "emails/layout/email.jte";

    private final TransactionalEmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailTextRenderer textRenderer;
    private final AppProperties appProperties;

    public void send(String recipient, String subject, EmailContentView content) {
        String html = templateRenderer.render(LAYOUT, Map.of("serviceUrl", baseUrl(), "content", content));

        emailSender.send(recipient, subject, html, textRenderer.render(content));
    }

    public String baseUrl() {
        String baseUrl = appProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }
}
