package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.view.EmailActionView;
import dev.gushchin.taskmanager.view.EmailContentView;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class VerificationEmailService {
    private static final String VERIFICATION_PATH_PREFIX = "/verify-email/";
    private static final String SUBJECT = "Подтверждение регистрации в TaskMePlease";
    private static final String HEADING = "Подтверждение регистрации";

    private final EmailMessageSender messageSender;
    private final EmailLifetimeFormatter lifetimeFormatter;
    private final EmailGreetingFormatter greetingFormatter;

    public void sendVerification(User user, String token, Duration lifetime, String invite) {
        EmailContentView content = EmailContentView.builder()
                .heading(HEADING)
                .bodyParagraph(greetingFormatter.format(user))
                .bodyParagraph("Чтобы завершить регистрацию в TaskMePlease, необходимо подтвердить почту.")
                .action(new EmailActionView("Подтвердить почту", buildUrl(token, invite)))
                .note("Ссылка действует " + lifetimeFormatter.formatHours(lifetime) + ".")
                .note("Если вы не регистрировались в TaskMePlease, просто проигнорируйте это письмо.")
                .build();

        messageSender.send(user.getEmail(), SUBJECT, content);
    }

    private String buildUrl(String token, String invite) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(messageSender.baseUrl())
                .path(VERIFICATION_PATH_PREFIX)
                .pathSegment(token);
        if (invite != null && !invite.isBlank()) {
            builder.queryParam("invite", invite);
        }

        return builder.build().encode().toUriString();
    }
}
