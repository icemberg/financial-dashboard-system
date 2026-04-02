package com.financedashboard.zorvyn.dto;

import com.financedashboard.zorvyn.enums.RolesEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO returned after successful authentication (both password and Google OAuth2 flows).
 * Contains the JWT token and basic profile info the frontend needs to bootstrap the session.
 * Password is never included in this or any other response DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** The JWT used for subsequent authenticated API calls. */
    private String token;

    /** Always "Bearer" — indicates how the token should be sent in the Authorization header. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Token lifetime in seconds (e.g., 86400 = 24 hours). */
    private long expiresIn;

    private Long userId;
    private String email;
    private String name;
    private RolesEnum role;
}
