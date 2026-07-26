package dev.gushchin.taskmanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    private static final String LOGIN_PATH = "/login";
    private static final String REGISTRATION_PATH = "/registration";
    private static final String RESEND_VERIFICATION_PATH = "/resend-verification";
    private static final String VERIFICATION_PENDING_PATH = "/verification-pending";

    private final CustomUserDetailsService userDetailsService;
    private final FlashAuthenticationFailureHandler authenticationFailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final SafeRedirectAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/",
                                FORGOT_PASSWORD_PATH,
                                LOGIN_PATH,
                                REGISTRATION_PATH,
                                RESEND_VERIFICATION_PATH,
                                VERIFICATION_PENDING_PATH,
                                "/reset-password/*",
                                "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/invitations/*", "/verify-email/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, REGISTRATION_PATH)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.loginPage(LOGIN_PATH)
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl(LOGIN_PATH).permitAll())
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }
}
