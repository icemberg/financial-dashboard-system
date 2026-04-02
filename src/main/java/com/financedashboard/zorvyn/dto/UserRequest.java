package com.financedashboard.zorvyn.dto;

import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for user creation and update requests.
 * Includes validation annotations for input validation.
 * Password is NOT included in response DTOs to prevent sensitive data leakage.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequest {

    /**
     * User's full name.
     * Required, must not be blank, max 100 characters
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /**
     * User's email address.
     * Required, must be valid email format, unique in database
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * User's password (for creation).
     * Required, must be between 8 and 50 characters
     * Should contain uppercase, lowercase, digit, and special character (validated in service)
     */
    @NotBlank(message = "Password is required for user creation")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    private String password;

    /**
     * User's role (VIEWER, ANALYST, ADMIN).
     * Required, determines access level and permissions
     */
    @NotNull(message = "Role is required")
    private RolesEnum role;

    /**
     * User's account status (ACTIVE or INACTIVE).
     * Required, controls whether user can access the system
     */
    @NotNull(message = "Status is required")
    private UserStatusEnum status;
}
