package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.exception.UserAlreadyExistsException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.SafeRedirectAuthenticationSuccessHandler;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.AuthenticationInviteView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private static final String EMAIL_FORMAT_ERROR_MESSAGE = "Email имеет неправильный формат.";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String INVITE_PARAMETER = "invite";
    private static final String LOGIN_PATH = "/login";
    private static final String PASSWORD_REQUIRED_ERROR_MESSAGE = "Пароль не заполнен.";
    private static final String REGISTRATION_PATH = "/registration";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String REDIRECT_TASKS = "redirect:/tasks";
    private static final String TASKS_PATH = "/tasks";
    private static final String REQUIRED_FIELDS_ERROR_MESSAGE = "Заполните обязательные поля.";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String USER_ALREADY_EXISTS_ERROR_MESSAGE = "Пользователь с таким email уже зарегистрирован.";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final TeamInvitationService teamInvitationService;
    private final TeamService teamService;
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String invite,
            Authentication authentication,
            Model model,
            CsrfToken csrfToken) {
        if (isAuthenticated(authentication)) {
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
    public String register(
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
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

        try {
            userService.create(email, name, password);
            authenticateRegisteredUser(email, password, request, response);
        } catch (UserAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, USER_ALREADY_EXISTS_ERROR_MESSAGE);
            return REDIRECT_PREFIX + buildAuthUrl(REGISTRATION_PATH, redirect, invite);
        }

        return REDIRECT_PREFIX + getRegistrationSuccessRedirect(redirect);
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

    private void authenticateRegisteredUser(
            String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    private String getRegistrationSuccessRedirect(String redirect) {
        String safeRedirect = getSafeRedirect(redirect);

        if (safeRedirect != null) {
            return safeRedirect;
        }

        return TASKS_PATH;
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

            return new AuthenticationInviteView(team.getName(), invitedBy.getName(), invitedBy.getEmail());
        } catch (TeamInvitationNotFoundException ex) {
            return null;
        } catch (TeamInvitationNotPendingException ex) {
            return null;
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
