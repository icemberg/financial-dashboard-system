package com.financedashboard.zorvyn.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.financedashboard.zorvyn.security.CustomUserDetailsService;
import com.financedashboard.zorvyn.security.JwtAuthFilter;
import com.financedashboard.zorvyn.security.OAuth2LoginSuccessHandler;

/**
 * Central Spring Security configuration.
 *
 * Auth strategies:
 * - Password login → POST /v1/auth/login → returns JWT
 * - Google OAuth2 → GET /oauth2/authorization/google → Google consent → JWT
 * redirect
 *
 * All API endpoints use JWT for authentication (stateless).
 * OAuth2 redirect cycle needs a brief session for the state parameter —
 * IF_REQUIRED handles this.
 * Role-based method security is enabled via @EnableMethodSecurity.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    /**
     * Comma-separated list of allowed CORS origins.
     * Local: set in application-local.properties
     * Prod: set via environment variable / AWS Secrets Manager
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // IF_REQUIRED: allows OAuth2 to store state in session during redirect cycle.
                // JWT auth is stateless — the filter does not create sessions for API calls.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints — no JWT required
                        .requestMatchers("/v1/auth/register", "/v1/auth/login").permitAll()
                        .requestMatchers("/v1/auth/forgot-password", "/v1/auth/reset-password").permitAll()
                        // Google OAuth2 flow (stateful redirect cycle)
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // API docs
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Health check — safe to expose publicly for load balancers
                        .requestMatchers("/actuator/health").permitAll()
                        // Everything else (including /v1/auth/refresh and /v1/auth/change-password) requires JWT
                        .anyRequest().authenticated())

                // Google OAuth2 login — success handler issues our JWT and redirects to
                // frontend
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler))

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS policy for all API routes.
     *
     * Allowed origins : driven by app.cors.allowed-origins property
     * (profile-specific)
     * Allowed methods : GET, POST, PATCH, DELETE, OPTIONS (OPTIONS needed for
     * preflight)
     * Allowed headers : Authorization (JWT) + Content-Type (JSON body)
     * Exposed headers : Authorization (so frontend can read it from response if
     * needed)
     * Allow credentials: true — required for browser to send Authorization header
     * Max age : 3600s — browser caches preflight for 1 hour, reducing OPTIONS calls
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7 (Spring Boot 4): constructor accepts UserDetailsService
        // directly
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
