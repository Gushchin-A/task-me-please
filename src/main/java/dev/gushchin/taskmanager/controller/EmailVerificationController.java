package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.InvalidAccountTokenException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.EmailVerificationResendResult;
import dev.gushchin.taskmanager.model.EmailVerificationResendState;
import dev.gushchin.taskmanager.model.EmailVerificationResult;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.security.EmailVerificationAuthenticationService;
import dev.gushchin.taskmanager.security.SafeRedirectAuthenticationSuccessHandler;
import dev.gushchin.taskmanager.service.EmailVerificationService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
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
    private static final String DELIVERY_FAILED_ATTRIBUTE = "deliveryFailed";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String INVITATIONS_PATH_PREFIX = "/invitations/";
    private static final String INVITE_PARAMETER = "invite";
    private static final String LOGIN_PATH = "/login";
    private static final String NEUTRAL_RESEND_MESSAGE = "Отправили вам новое письмо с подтверждением почты";
    private static final String RESEND_COOLDOWN_MESSAGE = "Письмо уже отправлено. Повторить можно через минуту";
    private static final String RESEND_LIMIT_MESSAGE =
            "Достигнут дневной лимит писем. Попробуйте запросить подтверждение завтра";
    private static final String VERIFICATION_EMAIL_FAILED_MESSAGE =
            "Не удалось отправить письмо с подтверждением регистрации. "
                    + "Проблема на нашей стороне, мы уже работаем над этим. Попробуйте позже";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String TASKS_PATH = "/tasks";
    private static final String VERIFICATION_AUTO_LOGIN_EMAIL_SESSION_ATTRIBUTE = "verificationAutoLoginEmail";
    private static final String VERIFICATION_EMAIL_SESSION_ATTRIBUTE = "verificationEmail";
    private static final String VERIFICATION_INVITE_SESSION_ATTRIBUTE = "verificationInvite";
    private static final String VERIFICATION_INVALID_MESSAGE =
            "Ссылка подтверждения почты недействительна или устарела";
    private static final String VERIFICATION_LOGIN_PARAMETER = "verification";
    private static final String VERIFICATION_PENDING_PATH = "/verification-pending";
    private static final String VERIFICATION_REDIRECT_SESSION_ATTRIBUTE = "verificationRedirect";
    private static final String VERIFICATION_SUCCESS_MESSAGE = "Email подтвержден";
    private static final String VERIFICATION_SUCCESS_LOGIN_MESSAGE = "Email подтвержден. Выполните вход";
    private static final String VERIFICATION_ALREADY_CONFIRMED_MESSAGE =
            "Ваш email уже подтвержден. Эта ссылка больше недействительна";
    private static final String VERIFICATION_ALREADY_CONFIRMED_LOGIN_MESSAGE =
            "Ваш email уже подтвержден. Выполните вход. Эта ссылка больше недействительна";

    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationAuthenticationService emailVerificationAuthenticationService;
    private final TeamInvitationService teamInvitationService;

    @GetMapping(VERIFICATION_PENDING_PATH)
    public String pending(HttpSession session, Model model, CsrfToken csrfToken) {
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
        model.addAttribute(
                DELIVERY_FAILED_ATTRIBUTE, Boolean.TRUE.equals(getModelAttribute(model, DELIVERY_FAILED_ATTRIBUTE)));
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, redirect);
        model.addAttribute(INVITE_PARAMETER, invite);
        model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, getModelAttribute(model, SUCCESS_MESSAGE_ATTRIBUTE));
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, getModelAttribute(model, ERROR_MESSAGE_ATTRIBUTE));

        return "verification-pending";
    }

    @PostMapping("/resend-verification")
    public String resend(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String invite,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String safeRedirect = getSafeRedirect(redirect);
        String inviteValue = getValidInvite(invite);
        String email = (String) session.getAttribute(VERIFICATION_EMAIL_SESSION_ATTRIBUTE);

        session.setAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE, safeRedirect);
        session.setAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE, inviteValue);

        EmailVerificationResendResult resendResult = email == null || email.isBlank()
                ? EmailVerificationResendResult.NOT_APPLICABLE
                : emailVerificationService.resend(email, inviteValue);
        addResendFlash(resendResult, redirectAttributes);

        return REDIRECT_PREFIX + VERIFICATION_PENDING_PATH;
    }

    private void addResendFlash(EmailVerificationResendResult result, RedirectAttributes redirectAttributes) {
        if (result == EmailVerificationResendResult.DELIVERY_FAILED) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, VERIFICATION_EMAIL_FAILED_MESSAGE);
            redirectAttributes.addFlashAttribute(DELIVERY_FAILED_ATTRIBUTE, true);
            return;
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, NEUTRAL_RESEND_MESSAGE);
    }

    @GetMapping("/verify-email/{token}")
    public String verify(
            @PathVariable String token,
            @RequestParam(required = false) String invite,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        try {
            EmailVerificationResult result = emailVerificationService.verify(token);
            String validInvite = getValidInvite(invite);
            if (result.alreadyVerified()) {
                return handleAlreadyVerified(result.user(), authentication, request, validInvite, redirectAttributes);
            }

            return handleVerified(result.user(), authentication, request, response, validInvite, redirectAttributes);
        } catch (InvalidAccountTokenException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, VERIFICATION_INVALID_MESSAGE);

            return REDIRECT_PREFIX + buildLoginUrl(null, null);
        }
    }

    private String handleVerified(
            User user,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            String invite,
            RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (isAuthenticatedAs(authentication, user)) {
            String target = getVerificationTarget(session, invite);
            clearVerificationContext(session);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, VERIFICATION_SUCCESS_MESSAGE);

            return REDIRECT_PREFIX + target;
        }

        if (canAuthenticate(session, authentication, user)) {
            final String target = getVerificationTarget(session, invite);
            emailVerificationAuthenticationService.authenticate(user, request, response);
            clearVerificationContext(request.getSession(false));
            saveSuccessMessage(request, VERIFICATION_SUCCESS_MESSAGE);

            return REDIRECT_PREFIX + target;
        }

        String loginUrl = getVerificationLoginUrl(session, invite);
        clearVerificationContext(session);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, VERIFICATION_SUCCESS_LOGIN_MESSAGE);

        return REDIRECT_PREFIX + loginUrl;
    }

    private String handleAlreadyVerified(
            User user,
            Authentication authentication,
            HttpServletRequest request,
            String invite,
            RedirectAttributes redirectAttributes) {
        if (isAuthenticatedAs(authentication, user)) {
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, VERIFICATION_ALREADY_CONFIRMED_MESSAGE);

            return REDIRECT_PREFIX + TASKS_PATH;
        }

        HttpSession session = request.getSession(false);
        String loginUrl = getVerificationLoginUrl(session, invite);
        clearVerificationContext(session);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, VERIFICATION_ALREADY_CONFIRMED_LOGIN_MESSAGE);

        return REDIRECT_PREFIX + loginUrl;
    }

    private boolean canAuthenticate(HttpSession session, Authentication authentication, User user) {
        if (isAuthenticated(authentication) || session == null) {
            return false;
        }

        String verificationEmail = (String) session.getAttribute(VERIFICATION_AUTO_LOGIN_EMAIL_SESSION_ATTRIBUTE);

        return verificationEmail != null && verificationEmail.equalsIgnoreCase(user.getEmail());
    }

    private boolean isAuthenticatedAs(Authentication authentication, User user) {
        return isAuthenticated(authentication)
                && authentication.getPrincipal() instanceof AuthUser authUser
                && authUser.getId().equals(user.getId());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String getVerificationTarget(HttpSession session, String invite) {
        if (session == null) {
            return invite == null ? TASKS_PATH : INVITATIONS_PATH_PREFIX + invite;
        }

        String redirect = getSafeRedirect((String) session.getAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE));
        if (redirect != null) {
            return redirect;
        }

        String sessionInvite = getValidInvite((String) session.getAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE));

        String targetInvite = sessionInvite == null ? invite : sessionInvite;

        return targetInvite == null ? TASKS_PATH : INVITATIONS_PATH_PREFIX + targetInvite;
    }

    private String getVerificationLoginUrl(HttpSession session, String invite) {
        if (session == null) {
            return buildLoginUrl(invite == null ? null : INVITATIONS_PATH_PREFIX + invite, invite);
        }

        String redirect = getSafeRedirect((String) session.getAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE));
        String sessionInvite = getValidInvite((String) session.getAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE));
        String targetInvite = sessionInvite == null ? invite : sessionInvite;
        String targetRedirect =
                redirect == null && targetInvite != null ? INVITATIONS_PATH_PREFIX + targetInvite : redirect;

        return buildLoginUrl(targetRedirect, targetInvite);
    }

    private void clearVerificationContext(HttpSession session) {
        if (session == null) {
            return;
        }

        session.removeAttribute(VERIFICATION_EMAIL_SESSION_ATTRIBUTE);
        session.removeAttribute(VERIFICATION_AUTO_LOGIN_EMAIL_SESSION_ATTRIBUTE);
        session.removeAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE);
        session.removeAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE);
    }

    private void saveSuccessMessage(HttpServletRequest request, String message) {
        request.getSession().setAttribute(VerificationMessageViewAdvice.SESSION_ATTRIBUTE, message);
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
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(LOGIN_PATH).queryParam(VERIFICATION_LOGIN_PARAMETER, true);
        if (redirect != null) {
            builder.queryParam(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, redirect);
        }

        if (invite != null) {
            builder.queryParam(INVITE_PARAMETER, invite);
        }

        return builder.build().encode().toUriString();
    }
}
