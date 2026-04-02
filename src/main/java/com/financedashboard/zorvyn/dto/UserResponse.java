package com.financedashboard.zorvyn.dto;

import java.time.LocalDateTime;

import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for returning user data in API responses.
 * IMPORTANT: Password field is intentionally excluded to prevent sensitive data exposure.
 * Only includes publicly safe data: id, name, email, role, status, timestamps.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    /**
     * Unique user identifier (database primary key).
     */
    private Long id;

    /**
     * User's full name.
     */
    private String name;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's role (VIEWER, ANALYST, ADMIN).
     * Determines access level and permissions within the system.
     */
    private RolesEnum role;

    /**
     * User's account status (ACTIVE or INACTIVE).
     * ACTIVE users can access the system; INACTIVE users are locked out.
     */
    private UserStatusEnum status;

    /**
     * Timestamp when user account was created.
     * Formatted as ISO 8601 datetime string in JSON responses.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Timestamp when user account was last updated.
     * Null if user has never been updated after creation.
     * Formatted as ISO 8601 datetime string in JSON responses.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
