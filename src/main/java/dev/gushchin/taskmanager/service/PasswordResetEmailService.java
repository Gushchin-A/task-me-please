package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.view.EmailActionView;
import dev.gushchin.taskmanager.view.EmailContentView;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {
    private static final String PASSWORD_RESET_PATH_PREFIX = "/reset-password/";
    private static final String SUBJECT = "Восстановление пароля в TaskMePlease";
    private static final String HEADING = "Восстановление пароля";

    private final EmailMessageSender messageSender;
    private final EmailLifetimeFormatter lifetimeFormatter;
    private final EmailGreetingFormatter greetingFormatter;

    public void sendPasswordReset(User user, String token, Duration lifetime) {
        EmailContentView content = EmailContentView.builder()
                .heading(HEADING)
                .bodyParagraph(greetingFormatter.format(user))
                .bodyParagraph("Мы получили запрос на изменение пароля вашего аккаунта.")
                .action(new EmailActionView(
                        "Установить новый пароль", messageSender.baseUrl() + PASSWORD_RESET_PATH_PREFIX + token))
                .note("Ссылка действует " + lifetimeFormatter.formatMinutes(lifetime) + ".")
                .note("Если вы не запрашивали изменение пароля, просто проигнорируйте это письмо.")
                .build();

        messageSender.send(user.getEmail(), SUBJECT, content);
    }
}
