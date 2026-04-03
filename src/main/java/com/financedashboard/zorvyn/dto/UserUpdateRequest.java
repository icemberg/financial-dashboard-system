package com.financedashboard.zorvyn.dto;

import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for partial user updates via PATCH /v1/users/{id}.
 *
 * All fields are optional — only non-null/non-blank values are applied by the service.
 * This fixes the Bug #1 where UserRequest forced all 5 fields (including password)
 * even when only a single field needed to change.
 *
 * Note: Password is intentionally excluded. Use PATCH /v1/auth/change-password instead.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    /**
     * User's updated display name.
     * Optional — if blank or null, the existing name is kept.
     */
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /**
     * User's updated email address.
     * Optional — if blank or null, the existing email is kept.
     * Must be unique — the service checks for duplicates before applying the change.
     */
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * User's updated role.
     * Optional — if null, the existing role is kept.
     * ADMIN only — the @PreAuthorize on the controller already enforces this.
     */
    private RolesEnum role;

    /**
     * User's updated account status.
     * Optional — if null, the existing status is kept.
     */
    private UserStatusEnum status;
}
