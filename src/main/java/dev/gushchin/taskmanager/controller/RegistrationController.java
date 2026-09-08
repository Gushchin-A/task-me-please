package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.exception.UserAlreadyExistsException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.SafeRedirectAuthenticationSuccessHandler;
import dev.gushchin.taskmanager.service.EmailVerificationService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.AuthenticationInviteView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
    private static final String CSRF_ATTRIBUTE = "_csrf";
    private static final String EMAIL_FORMAT_ERROR_MESSAGE = "Email имеет неправильный формат";
    private static final String DELIVERY_FAILED_ATTRIBUTE = "deliveryFailed";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String INVITE_PARAMETER = "invite";
    private static final String LOGIN_PATH = "/login";
    private static final String PASSWORD_REQUIRED_ERROR_MESSAGE = "Пароль не заполнен";
    private static final String REGISTRATION_PATH = "/registration";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String REDIRECT_TASKS = "redirect:/tasks";
    private static final String REQUIRED_FIELDS_ERROR_MESSAGE = "Заполните обязательные поля";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String UNVERIFIED_EMAIL_ATTRIBUTE = "unverifiedEmail";
    private static final String USER_ALREADY_EXISTS_ERROR_MESSAGE = "Пользователь с таким email уже зарегистрирован";
    private static final String VERIFICATION_AUTO_LOGIN_EMAIL_SESSION_ATTRIBUTE = "verificationAutoLoginEmail";
    private static final String VERIFICATION_EMAIL_SESSION_ATTRIBUTE = "verificationEmail";
    private static final String VERIFICATION_INVITE_SESSION_ATTRIBUTE = "verificationInvite";
    private static final String VERIFICATION_PENDING_REDIRECT = "redirect:/verification-pending";
    private static final String VERIFICATION_EMAIL_FAILED_MESSAGE =
            "Не удалось отправить письмо с подтверждением регистрации. "
                    + "Проблема на нашей стороне, мы уже работаем над этим. Попробуйте позже";
    private static final String VERIFICATION_REDIRECT_SESSION_ATTRIBUTE = "verificationRedirect";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final EmailVerificationService emailVerificationService;
    private final TeamInvitationService teamInvitationService;
    private final TeamService teamService;
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String invite,
            @RequestParam(defaultValue = "false") boolean verification,
            Authentication authentication,
            Model model,
            CsrfToken csrfToken) {
        if (isAuthenticated(authentication) && !verification) {
            return REDIRECT_TASKS;
        }

        addAuthAttributes(model, csrfToken, redirect, invite);

        return "login";
    }

    @GetMapping("/registration")
    public String registrationPage(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String invite,
            Authentication authentication,
            Model model,
            CsrfToken csrfToken) {
        if (isAuthenticated(authentication)) {
            return REDIRECT_TASKS;
        }

        addAuthAttributes(model, csrfToken, redirect, invite);

        return "registration";
    }

    @PostMapping("/registration")
    public String register(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String email = request.getParameter("email");
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        String redirect = request.getParameter(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER);
        String invite = request.getParameter(INVITE_PARAMETER);
        String errorMessage = validateRegistration(email, password);

        if (errorMessage != null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, errorMessage);
            return REDIRECT_PREFIX + buildAuthUrl(REGISTRATION_PATH, redirect, invite);
        }

        boolean emailDelivered;
        try {
            emailDelivered = emailVerificationService.register(email, name, password, getInvite(invite));
        } catch (UserAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, USER_ALREADY_EXISTS_ERROR_MESSAGE);
            return REDIRECT_PREFIX + buildAuthUrl(REGISTRATION_PATH, redirect, invite);
        }

        if (!emailDelivered) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, VERIFICATION_EMAIL_FAILED_MESSAGE);
            redirectAttributes.addFlashAttribute(DELIVERY_FAILED_ATTRIBUTE, true);
        }

        saveVerificationContext(request.getSession(), email, redirect, invite);

        return VERIFICATION_PENDING_REDIRECT;
    }

    private void addAuthAttributes(Model model, CsrfToken csrfToken, String redirect, String invite) {
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        model.addAttribute(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, getSafeRedirect(redirect));
        model.addAttribute(INVITE_PARAMETER, getInvite(invite));
        model.addAttribute("inviteContext", getInviteContext(invite));
        model.addAttribute("loginUrl", buildAuthUrl(LOGIN_PATH, redirect, invite));
        model.addAttribute("registrationUrl", buildAuthUrl(REGISTRATION_PATH, redirect, invite));

        if (!model.containsAttribute(SUCCESS_MESSAGE_ATTRIBUTE)) {
            model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, null);
        }

        if (!model.containsAttribute(ERROR_MESSAGE_ATTRIBUTE)) {
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, null);
        }
        if (!model.containsAttribute(UNVERIFIED_EMAIL_ATTRIBUTE)) {
            model.addAttribute(UNVERIFIED_EMAIL_ATTRIBUTE, null);
        }
    }

    private String getSafeRedirect(String redirect) {
        if (SafeRedirectAuthenticationSuccessHandler.isSafeRedirect(redirect)) {
            return redirect;
        }

        return null;
    }

    private String getInvite(String invite) {
        if (invite != null && !invite.isBlank()) {
            return invite;
        }

        return null;
    }

    private AuthenticationInviteView getInviteContext(String invite) {
        String inviteToken = getInvite(invite);
        if (inviteToken == null) {
            return null;
        }

        try {
            TeamInvitation invitation = teamInvitationService.findPendingByToken(inviteToken);
            Team team = teamService.findById(invitation.getTeamId());
            User invitedBy = userService.findById(invitation.getInvitedBy());

            return new AuthenticationInviteView(team.getName(), invitedBy.getEmail());
        } catch (TeamInvitationNotFoundException ex) {
            return null;
        } catch (TeamInvitationNotPendingException ex) {
            return null;
        }
    }

    private String validateRegistration(String email, String password) {
        if (email == null || email.isBlank()) {
            return REQUIRED_FIELDS_ERROR_MESSAGE;
        }

        if (password == null || password.isBlank()) {
            return PASSWORD_REQUIRED_ERROR_MESSAGE;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return EMAIL_FORMAT_ERROR_MESSAGE;
        }

        return null;
    }

    private String buildAuthUrl(String path, String redirect, String invite) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        String safeRedirect = getSafeRedirect(redirect);
        String inviteValue = getInvite(invite);

        if (safeRedirect != null) {
            builder.queryParam(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, safeRedirect);
        }

        if (inviteValue != null) {
            builder.queryParam(INVITE_PARAMETER, inviteValue);
        }

        return builder.build().encode().toUriString();
    }

    private void saveVerificationContext(HttpSession session, String email, String redirect, String invite) {
        session.setAttribute(VERIFICATION_AUTO_LOGIN_EMAIL_SESSION_ATTRIBUTE, email);
        session.setAttribute(VERIFICATION_EMAIL_SESSION_ATTRIBUTE, email);
        session.setAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE, getSafeRedirect(redirect));
        session.setAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE, getInvite(invite));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
