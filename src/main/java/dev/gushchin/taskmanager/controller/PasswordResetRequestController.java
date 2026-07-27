package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.service.PasswordResetRequestService;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetRequestController {
    private static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    private static final String NEUTRAL_RESULT_MESSAGE =
            "Если аккаунт с таким email существует, мы отправили ссылку для восстановления password.";
    private static final String REQUEST_ATTEMPTS_SESSION_ATTRIBUTE = "passwordResetRequestAttempts";
    private static final String REQUEST_AVAILABLE_AT_SESSION_ATTRIBUTE = "passwordResetRequestAvailableAt";
    private static final String REQUEST_EMAIL_SESSION_ATTRIBUTE = "passwordResetRequestEmail";
    private static final String REQUEST_WINDOW_STARTED_AT_SESSION_ATTRIBUTE = "passwordResetRequestWindowStartedAt";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";

    private final PasswordResetRequestService passwordResetRequestService;

    @GetMapping(FORGOT_PASSWORD_PATH)
    public String forgotPassword(HttpSession session, Model model, CsrfToken csrfToken) {
        PasswordResetRequestState requestState = getRequestState(session, Instant.now());

        model.addAttribute("_csrf", csrfToken);
        model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, getSuccessMessage(model));
        model.addAttribute("errorMessage", null);
        model.addAttribute("loginUrl", "/login");
        model.addAttribute("registrationUrl", "/registration");
        model.addAttribute("email", session.getAttribute(REQUEST_EMAIL_SESSION_ATTRIBUTE));
        model.addAttribute("cooldownSeconds", requestState.cooldownSeconds());
        model.addAttribute("remainingAttempts", requestState.remainingAttempts());
        model.addAttribute("requestAvailable", requestState.available());

        return "forgot-password";
    }

    @PostMapping(FORGOT_PASSWORD_PATH)
    public String requestPasswordReset(
            @RequestParam(required = false) String email, HttpSession session, RedirectAttributes redirectAttributes) {
        if (registerAttempt(session, email, Instant.now())) {
            passwordResetRequestService.request(email);
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, NEUTRAL_RESULT_MESSAGE);

        return "redirect:" + FORGOT_PASSWORD_PATH;
    }

    private Object getSuccessMessage(Model model) {
        if (model.containsAttribute(SUCCESS_MESSAGE_ATTRIBUTE)) {
            return model.getAttribute(SUCCESS_MESSAGE_ATTRIBUTE);
        }

        return null;
    }

    private boolean registerAttempt(HttpSession session, String email, Instant now) {
        if (email == null || email.isBlank()) {
            return false;
        }

        resetSessionStateForAnotherEmail(session, email);
        PasswordResetRequestState state = getRequestState(session, now);
        if (!state.available()) {
            return false;
        }

        session.setAttribute(REQUEST_EMAIL_SESSION_ATTRIBUTE, email);
        session.setAttribute(REQUEST_ATTEMPTS_SESSION_ATTRIBUTE, getAttempts(session) + 1);
        session.setAttribute(
                REQUEST_AVAILABLE_AT_SESSION_ATTRIBUTE, now.plus(PasswordResetRequestService.REQUEST_COOLDOWN));
        if (session.getAttribute(REQUEST_WINDOW_STARTED_AT_SESSION_ATTRIBUTE) == null) {
            session.setAttribute(REQUEST_WINDOW_STARTED_AT_SESSION_ATTRIBUTE, now);
        }

        return true;
    }

    private PasswordResetRequestState getRequestState(HttpSession session, Instant now) {
        resetExpiredWindow(session, now);
        int remainingAttempts = Math.max(0, PasswordResetRequestService.MAX_REQUEST_ATTEMPTS - getAttempts(session));
        int cooldownSeconds = getCooldownSeconds(session, now);

        return new PasswordResetRequestState(cooldownSeconds, remainingAttempts);
    }

    private void resetSessionStateForAnotherEmail(HttpSession session, String email) {
        String sessionEmail = (String) session.getAttribute(REQUEST_EMAIL_SESSION_ATTRIBUTE);
        if (sessionEmail != null && !Objects.equals(sessionEmail, email)) {
            clearRequestState(session);
        }
    }

    private void resetExpiredWindow(HttpSession session, Instant now) {
        Instant windowStartedAt = (Instant) session.getAttribute(REQUEST_WINDOW_STARTED_AT_SESSION_ATTRIBUTE);
        if (windowStartedAt != null
                && !windowStartedAt
                        .plus(PasswordResetRequestService.REQUEST_LIMIT_WINDOW)
                        .isAfter(now)) {
            clearRequestState(session);
        }
    }

    private void clearRequestState(HttpSession session) {
        session.removeAttribute(REQUEST_ATTEMPTS_SESSION_ATTRIBUTE);
        session.removeAttribute(REQUEST_AVAILABLE_AT_SESSION_ATTRIBUTE);
        session.removeAttribute(REQUEST_EMAIL_SESSION_ATTRIBUTE);
        session.removeAttribute(REQUEST_WINDOW_STARTED_AT_SESSION_ATTRIBUTE);
    }

    private int getAttempts(HttpSession session) {
        Integer attempts = (Integer) session.getAttribute(REQUEST_ATTEMPTS_SESSION_ATTRIBUTE);

        return attempts == null ? 0 : attempts;
    }

    private int getCooldownSeconds(HttpSession session, Instant now) {
        Instant availableAt = (Instant) session.getAttribute(REQUEST_AVAILABLE_AT_SESSION_ATTRIBUTE);
        if (availableAt == null || !availableAt.isAfter(now)) {
            return 0;
        }

        Duration remaining = Duration.between(now, availableAt);
        long seconds = remaining.getSeconds();
        if (remaining.getNano() > 0) {
            seconds++;
        }

        return Math.toIntExact(seconds);
    }

    private record PasswordResetRequestState(int cooldownSeconds, int remainingAttempts) {
        private boolean available() {
            return cooldownSeconds == 0 && remainingAttempts > 0;
        }
    }
}
