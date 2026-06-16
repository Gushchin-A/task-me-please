package dev.gushchin.taskmanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String REGISTRATION_PATH = "/registration";

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.requestMatchers("/", REGISTRATION_PATH, "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, REGISTRATION_PATH)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/tasks", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }
}
