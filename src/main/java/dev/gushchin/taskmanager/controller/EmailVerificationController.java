package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.InvalidAccountTokenException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.EmailVerificationResendState;
import dev.gushchin.taskmanager.security.SafeRedirectAuthenticationSuccessHandler;
import dev.gushchin.taskmanager.service.EmailVerificationService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class EmailVerificationController {
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String INVITE_PARAMETER = "invite";
    private static final String LOGIN_PATH = "/login";
    private static final String NEUTRAL_RESEND_MESSAGE =
            "Если аккаунт с таким email существует, мы отправили письмо для подтверждения.";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String VERIFICATION_EMAIL_SESSION_ATTRIBUTE = "verificationEmail";
    private static final String VERIFICATION_INVITE_SESSION_ATTRIBUTE = "verificationInvite";
    private static final String VERIFICATION_INVALID_MESSAGE = "Ссылка подтверждения недействительна или устарела.";
    private static final String VERIFICATION_PENDING_PATH = "/verification-pending";
    private static final String VERIFICATION_REDIRECT_SESSION_ATTRIBUTE = "verificationRedirect";
    private static final String VERIFICATION_SUCCESS_MESSAGE = "Email подтверждён. Теперь вы можете войти.";

    private final EmailVerificationService emailVerificationService;
    private final TeamInvitationService teamInvitationService;

    @GetMapping(VERIFICATION_PENDING_PATH)
    public String pending(HttpSession session, Model model) {
        String email = (String) session.getAttribute(VERIFICATION_EMAIL_SESSION_ATTRIBUTE);
        if (email == null || email.isBlank()) {
            return "redirect:/registration";
        }

        String redirect = getSafeRedirect((String) session.getAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE));
        String invite = getValidInvite((String) session.getAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE));
        EmailVerificationResendState resendState = emailVerificationService.getResendState(email);
        model.addAttribute("loginUrl", buildLoginUrl(redirect, invite));
        model.addAttribute("registrationUrl", "/registration");
        model.addAttribute("limitReached", resendState.remainingAttempts() == 0);
        model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, getModelAttribute(model, SUCCESS_MESSAGE_ATTRIBUTE));
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, getModelAttribute(model, ERROR_MESSAGE_ATTRIBUTE));

        return "verification-pending";
    }

    @PostMapping("/resend-verification")
    public String resend(
            @RequestParam String email,
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String invite,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String safeRedirect = getSafeRedirect(redirect);
        String inviteValue = getValidInvite(invite);

        emailVerificationService.resend(email, inviteValue);
        session.setAttribute(VERIFICATION_EMAIL_SESSION_ATTRIBUTE, email);
        session.setAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE, safeRedirect);
        session.setAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE, inviteValue);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, NEUTRAL_RESEND_MESSAGE);

        return REDIRECT_PREFIX + VERIFICATION_PENDING_PATH;
    }

    @GetMapping("/verify-email/{token}")
    public String verify(
            @PathVariable String token,
            @RequestParam(required = false) String invite,
            RedirectAttributes redirectAttributes) {
        try {
            emailVerificationService.verify(token);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, VERIFICATION_SUCCESS_MESSAGE);

            String validInvite = getValidInvite(invite);

            return REDIRECT_PREFIX
                    + buildLoginUrl(validInvite == null ? null : "/invitations/" + validInvite, validInvite);
        } catch (InvalidAccountTokenException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, VERIFICATION_INVALID_MESSAGE);

            return REDIRECT_PREFIX + LOGIN_PATH;
        }
    }

    private Object getModelAttribute(Model model, String attribute) {
        if (model.containsAttribute(attribute)) {
            return model.getAttribute(attribute);
        }

        return null;
    }

    private String getSafeRedirect(String redirect) {
        if (SafeRedirectAuthenticationSuccessHandler.isSafeRedirect(redirect)) {
            return redirect;
        }

        return null;
    }

    private String getValidInvite(String invite) {
        if (invite == null || invite.isBlank()) {
            return null;
        }

        try {
            teamInvitationService.findPendingByToken(invite);
            return invite;
        } catch (TeamInvitationNotFoundException ex) {
            return null;
        } catch (TeamInvitationNotPendingException ex) {
            return null;
        }
    }

    private String buildLoginUrl(String redirect, String invite) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(LOGIN_PATH);
        if (redirect != null) {
            builder.queryParam(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, redirect);
        }

        if (invite != null) {
            builder.queryParam(INVITE_PARAMETER, invite);
        }

        return builder.build().encode().toUriString();
    }
}
