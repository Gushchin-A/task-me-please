package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.User;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class VerificationEmailService {
    private static final String VERIFICATION_PATH_PREFIX = "/verify-email/";

    private final TransactionalEmailSender emailSender;
    private final AppProperties appProperties;

    public void sendVerification(User user, String token, Duration lifetime, String invite) {
        emailSender.send(user.getEmail(), "Подтвердите email в Task Me Please", buildText(token, lifetime, invite));
    }

    private String buildText(String token, Duration lifetime, String invite) {
        return "Добро пожаловать в Task Me Please.\n\n"
                + "Чтобы завершить регистрацию, подтвердите email по ссылке:\n"
                + buildUrl(token, invite) + "\n\n"
                + "Ссылка действует " + lifetime.toHours() + " часов.\n"
                + "Если вы не регистрировались в Task Me Please, проигнорируйте это письмо.";
    }

    private String buildUrl(String token, String invite) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl())
                .path(VERIFICATION_PATH_PREFIX)
                .pathSegment(token);
        if (invite != null && !invite.isBlank()) {
            builder.queryParam("invite", invite);
        }

        return builder.build().encode().toUriString();
    }

    private String getBaseUrl() {
        String baseUrl = appProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }
}
