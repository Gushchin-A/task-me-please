package dev.gushchin.taskmanager.security;

import dev.gushchin.taskmanager.config.AppProperties;
import jakarta.servlet.http.Cookie;
import javax.sql.DataSource;
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
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private static final int REMEMBER_ME_VALIDITY_SECONDS = 30 * 24 * 60 * 60;
    private static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    private static final String LOGIN_PATH = "/login";
    private static final String REGISTRATION_PATH = "/registration";
    private static final String RESEND_VERIFICATION_PATH = "/resend-verification";
    private static final String VERIFICATION_PENDING_PATH = "/verification-pending";

    private final CustomUserDetailsService userDetailsService;
    private final AppProperties appProperties;
    private final FlashAuthenticationFailureHandler authenticationFailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final SafeRedirectAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RememberMeServices rememberMeServices)
            throws Exception {
        return http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/",
                                FORGOT_PASSWORD_PATH,
                                LOGIN_PATH,
                                REGISTRATION_PATH,
                                RESEND_VERIFICATION_PATH,
                                VERIFICATION_PENDING_PATH,
                                "/css/**",
                                "/images/**",
                                "/js/**",
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
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
                .logout(logout -> logout.logoutSuccessUrl(LOGIN_PATH).permitAll())
                .build();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);

        return tokenRepository;
    }

    @Bean
    public RememberMeServices rememberMeServices(PersistentTokenRepository tokenRepository) {
        PersistentTokenBasedRememberMeServices rememberMeServices = new PersistentTokenBasedRememberMeServices(
                appProperties.getSecurity().getRememberMeKey(), userDetailsService, tokenRepository);
        rememberMeServices.setTokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
        rememberMeServices.setUseSecureCookie(appProperties.getBaseUrl().startsWith("https://"));
        rememberMeServices.setCookieCustomizer(this::customizeRememberMeCookie);

        return rememberMeServices;
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

    private void customizeRememberMeCookie(Cookie cookie) {
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
    }
}
