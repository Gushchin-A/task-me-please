package dev.gushchin.taskmanager.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FlashAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String INVITE_PARAMETER = "invite";
    private static final String LOGIN_ERROR_MESSAGE = "Неверный email или пароль";
    private static final String LOGIN_PATH = "/login";
    private static final String UNVERIFIED_EMAIL_ATTRIBUTE = "unverifiedEmail";
    private static final String UNVERIFIED_EMAIL_ERROR_MESSAGE =
            "Email не подтвержден. Проверьте почту или отправьте письмо повторно";
    private static final String USERNAME_PARAMETER = "username";
    private static final String VERIFICATION_EMAIL_SESSION_ATTRIBUTE = "verificationEmail";
    private static final String VERIFICATION_INVITE_SESSION_ATTRIBUTE = "verificationInvite";
    private static final String VERIFICATION_REDIRECT_SESSION_ATTRIBUTE = "verificationRedirect";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager());
        Map<String, Object> flashMap = RequestContextUtils.getOutputFlashMap(request);
        if (exception instanceof DisabledException) {
            flashMap.put(ERROR_MESSAGE_ATTRIBUTE, UNVERIFIED_EMAIL_ERROR_MESSAGE);
            flashMap.put(UNVERIFIED_EMAIL_ATTRIBUTE, request.getParameter(USERNAME_PARAMETER));
            saveVerificationContext(request);
        } else {
            flashMap.put(ERROR_MESSAGE_ATTRIBUTE, LOGIN_ERROR_MESSAGE);
        }

        String redirectUrl = getRedirectUrl(request);
        RequestContextUtils.saveOutputFlashMap(redirectUrl, request, response);

        response.sendRedirect(redirectUrl);
    }

    private void saveVerificationContext(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute(VERIFICATION_EMAIL_SESSION_ATTRIBUTE, request.getParameter(USERNAME_PARAMETER));

        String redirect = request.getParameter(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER);
        if (SafeRedirectAuthenticationSuccessHandler.isSafeRedirect(redirect)) {
            session.setAttribute(VERIFICATION_REDIRECT_SESSION_ATTRIBUTE, redirect);
        }

        String invite = request.getParameter(INVITE_PARAMETER);
        if (invite != null && !invite.isBlank()) {
            session.setAttribute(VERIFICATION_INVITE_SESSION_ATTRIBUTE, invite);
        }
    }

    private String getRedirectUrl(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(LOGIN_PATH);
        String redirect = request.getParameter(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER);
        String invite = request.getParameter(INVITE_PARAMETER);

        if (SafeRedirectAuthenticationSuccessHandler.isSafeRedirect(redirect)) {
            builder.queryParam(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, redirect);
        }

        if (invite != null && !invite.isBlank()) {
            builder.queryParam(INVITE_PARAMETER, invite);
        }

        return builder.build().encode().toUriString();
    }
}
