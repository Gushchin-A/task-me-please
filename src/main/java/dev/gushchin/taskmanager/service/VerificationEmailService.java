package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.User;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationEmailService {
    private static final String VERIFICATION_PATH_PREFIX = "/verify-email/";

    private final TransactionalEmailSender emailSender;
    private final AppProperties appProperties;

    public void sendVerification(User user, String token, Duration lifetime) {
        emailSender.send(user.getEmail(), "Подтвердите email в Task Me Please", buildText(token, lifetime));
    }

    private String buildText(String token, Duration lifetime) {
        return "Добро пожаловать в Task Me Please.\n\n"
                + "Чтобы завершить регистрацию, подтвердите email по ссылке:\n"
                + buildUrl(token) + "\n\n"
                + "Ссылка действует " + lifetime.toHours() + " часов.\n"
                + "Если вы не регистрировались в Task Me Please, проигнорируйте это письмо.";
    }

    private String buildUrl(String token) {
        return getBaseUrl() + VERIFICATION_PATH_PREFIX + token;
    }

    private String getBaseUrl() {
        String baseUrl = appProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }
}
