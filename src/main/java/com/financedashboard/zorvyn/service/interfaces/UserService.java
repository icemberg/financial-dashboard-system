package com.financedashboard.zorvyn.service.interfaces;

import java.util.List;

import com.financedashboard.zorvyn.dto.UserRequest;
import com.financedashboard.zorvyn.dto.UserResponse;
import com.financedashboard.zorvyn.dto.UserUpdateRequest;
import com.financedashboard.zorvyn.enums.UserStatusEnum;

/**
 * Service interface for user management operations.
 * All role-based access control is enforced at the controller layer via @PreAuthorize.
 */
public interface UserService {

    /** Returns all users. ADMIN only. */
    List<UserResponse> getAllUsers();

    /** Returns a user by ID. ADMIN or own profile. */
    UserResponse getUserById(Long id);

    /** Returns a user by email. Used internally for auth and duplicate checks. */
    UserResponse getUserByEmail(String email);

    /**
     * Creates a new user with a specified role and status.
     * ADMIN only — use /v1/auth/register for self-registration (always VIEWER).
     * BCrypt-encodes the password before storing.
     */
    UserResponse createUser(UserRequest userRequest);

    /**
     * Partially updates an existing user's profile fields.
     * Only non-null/non-blank fields in UserUpdateRequest are applied.
     * Password is NOT updated through this method — use PATCH /v1/auth/change-password.
     *
     * @param id            the user to update
     * @param updateRequest fields to update (all optional)
     */
    UserResponse updateUser(Long id, UserUpdateRequest updateRequest);

    /**
     * Updates only the user's account status (ACTIVE / INACTIVE).
     * ADMIN only.
     */
    UserResponse updateUserStatus(Long id, UserStatusEnum status);

    /** Hard-deletes a user. ADMIN only. */
    void deleteUser(Long id);

    /** Returns true if the given email is already registered. */
    boolean emailExists(String email);

    /** Returns true if the user exists and their status is ACTIVE. */
    boolean isUserActive(Long id);
}
