package com.asiandoor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

import com.asiandoor.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // activates @PreAuthorize / @PostAuthorize on beans
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    /**
     * Wires the DB-backed UserDetailsService and BCrypt encoder into a
     * DaoAuthenticationProvider so login credentials are validated against
     * the users table.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Custom 403 handler: sends the user to /access-denied instead of a blank error
        AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();
        accessDeniedHandler.setErrorPage("/access-denied");

        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/cart/**", "/orders/**")
            )
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // ── Public pages ────────────────────────────────────────────
                .requestMatchers("/", "/products", "/products/**",
                                 "/login", "/register", "/error", "/access-denied",
                                 "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                // ── Admin only — first line of defence ──────────────────────
                // Role stored in DB as "ROLE_ADMIN"; hasRole() strips the prefix.
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // ── Everything else requires login ──────────────────────────
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    String targetUrl = isAdmin ? "/admin" : "/";
                    response.sendRedirect(request.getContextPath() + targetUrl);
                })
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .rememberMe(rememberMe -> rememberMe
                .key("asiandoor-remember-me")
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
