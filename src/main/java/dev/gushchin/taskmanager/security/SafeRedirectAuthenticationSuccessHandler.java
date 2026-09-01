package dev.gushchin.taskmanager.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

@Component
public class SafeRedirectAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    public static final String REDIRECT_PARAMETER = "redirect";

    private static final String DEFAULT_SUCCESS_URL = "/tasks";

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        clearAuthenticationAttributes(request);

        redirectStrategy.sendRedirect(request, response, getTargetUrl(request, response));
    }

    public static boolean isSafeRedirect(String redirect) {
        if (!StringUtils.hasText(redirect)) {
            return false;
        }

        try {
            String decodedRedirect = UriUtils.decode(redirect, StandardCharsets.UTF_8);
            URI redirectUri = new URI(decodedRedirect);
            String lowerCaseRedirect = decodedRedirect.toLowerCase(Locale.ROOT);

            return decodedRedirect.startsWith("/")
                    && !decodedRedirect.startsWith("//")
                    && !decodedRedirect.contains("\\")
                    && !lowerCaseRedirect.contains("%2f")
                    && !lowerCaseRedirect.contains("%5c")
                    && !lowerCaseRedirect.contains("%25")
                    && decodedRedirect.chars().noneMatch(Character::isISOControl)
                    && !redirectUri.isAbsolute()
                    && redirectUri.getRawAuthority() == null
                    && redirectUri.getRawFragment() == null;
        } catch (IllegalArgumentException | URISyntaxException ex) {
            return false;
        }
    }

    private String getTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String redirect = request.getParameter(REDIRECT_PARAMETER);

        if (isSafeRedirect(redirect)) {
            return redirect;
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (savedRequest != null) {
            return savedRequest.getRedirectUrl();
        }

        return DEFAULT_SUCCESS_URL;
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }
}
