package dev.gushchin.taskmanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import dev.gushchin.taskmanager.config.AppProperties;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

class SecurityConfigTest {
    @Test
    void productionRememberMeCookieShouldUseSecureAttributes() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://taskmeplease.online");
        appProperties.getSecurity().setRememberMeKey("test-only-remember-me-key");
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        SecurityConfig securityConfig = new SecurityConfig(
                userDetailsService,
                appProperties,
                mock(FlashAuthenticationFailureHandler.class),
                mock(PasswordEncoder.class),
                mock(SafeRedirectAuthenticationSuccessHandler.class));
        RememberMeServices rememberMeServices =
                securityConfig.rememberMeServices(mock(PersistentTokenRepository.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("remember-me", "on");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated("user@test.com", null, List.of());

        rememberMeServices.loginSuccess(request, response, authentication);

        Cookie cookie = response.getCookie("remember-me");
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertEquals(30 * 24 * 60 * 60, cookie.getMaxAge());
    }
}
