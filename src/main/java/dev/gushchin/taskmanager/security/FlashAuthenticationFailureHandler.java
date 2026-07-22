package dev.gushchin.taskmanager.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private static final String LOGIN_ERROR_MESSAGE = "Неверный email или пароль.";
    private static final String LOGIN_PATH = "/login";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager());
        RequestContextUtils.getOutputFlashMap(request).put(ERROR_MESSAGE_ATTRIBUTE, LOGIN_ERROR_MESSAGE);

        String redirectUrl = getRedirectUrl(request);
        RequestContextUtils.saveOutputFlashMap(redirectUrl, request, response);

        response.sendRedirect(redirectUrl);
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
