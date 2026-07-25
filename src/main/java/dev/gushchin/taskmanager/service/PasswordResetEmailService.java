package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.User;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {
    private static final String PASSWORD_RESET_PATH_PREFIX = "/reset-password/";

    private final TransactionalEmailSender emailSender;
    private final AppProperties appProperties;

    public void sendPasswordReset(User user, String token, Duration lifetime) {
        emailSender.send(user.getEmail(), "Восстановление password в Task Me Please", buildText(token, lifetime));
    }

    private String buildText(String token, Duration lifetime) {
        return "Мы получили запрос на изменение password в Task Me Please.\n\n"
                + "Установить новый password:\n"
                + buildUrl(token) + "\n\n"
                + "Ссылка действует " + lifetime.toMinutes() + " минут.\n"
                + "Если вы не запрашивали изменение password, проигнорируйте это письмо.";
    }

    private String buildUrl(String token) {
        return getBaseUrl() + PASSWORD_RESET_PATH_PREFIX + token;
    }

    private String getBaseUrl() {
        String baseUrl = appProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }
}
