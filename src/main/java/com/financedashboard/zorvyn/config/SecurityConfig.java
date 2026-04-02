package com.financedashboard.zorvyn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security Configuration for the Finance Dashboard application.
 * Provides bean definitions for security components like PasswordEncoder.
 *
 * This class is responsible for configuring Spring Security beans
 * that are used throughout the application.
 */
@Configuration
public class SecurityConfig {

    /**
     * Provides BCryptPasswordEncoder bean for password encryption.
     * BCrypt is used for secure password hashing with:
     * - Adaptive strength (can adjust rounds over time)
     * - Built-in salt generation
     * - Protection against rainbow table attacks
     * - Industry standard for password storage
     *
     * @return PasswordEncoder bean with BCrypt implementation
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
