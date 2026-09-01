package dev.gushchin.taskmanager.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafeRedirectAuthenticationSuccessHandlerTest {
    @Test
    void shouldAllowApplicationRelativeRedirects() {
        assertTrue(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/tasks"));
        assertTrue(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/invitations/token?source=email"));
    }

    @Test
    void shouldRejectExternalAndAmbiguousRedirects() {
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect(null));
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("https://example.com"));
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("//example.com"));
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/\\example.com"));
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/%5Cexample.com"));
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/%255Cexample.com"));
        assertFalse(SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/tasks#section"));
        assertFalse(
                SafeRedirectAuthenticationSuccessHandler.isSafeRedirect("/tasks%0d%0aLocation:%20https://example.com"));
    }
}
