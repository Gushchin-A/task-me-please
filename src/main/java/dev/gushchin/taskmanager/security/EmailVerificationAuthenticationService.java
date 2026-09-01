package dev.gushchin.taskmanager.security;

import dev.gushchin.taskmanager.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationAuthenticationService {
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public void authenticate(User user, HttpServletRequest request, HttpServletResponse response) {
        AuthUser authUser = new AuthUser(user);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(authUser, null, authUser.getAuthorities());

        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }
}
