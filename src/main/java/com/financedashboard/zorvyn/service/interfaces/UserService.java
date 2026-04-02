package com.financedashboard.zorvyn.service.interfaces;

import java.util.List;

import com.financedashboard.zorvyn.dto.UserRequest;
import com.financedashboard.zorvyn.dto.UserResponse;
import com.financedashboard.zorvyn.enums.UserStatusEnum;

/**
 * Service interface for user management operations.
 * Defines contract for all user-related business logic.
 * Implementation handles database operations, validation, and security.
 *
 * Security Note: All methods enforce role-based access control via @PreAuthorize in controller.
 */
public interface UserService {

    /**
     * Retrieves all users in the system.
     * ADMIN only access.
     *
     * @return List of UserResponse containing all users (sensitive fields excluded)
     * @throws UserException if user list cannot be retrieved
     */
    List<UserResponse> getAllUsers();

    /**
     * Retrieves a specific user by their ID.
     * ADMIN can retrieve any user; non-admin can retrieve only their own data.
     *
     * @param id the user ID to retrieve
     * @return UserResponse containing user data
     * @throws UserException if user not found or access denied
     */
    UserResponse getUserById(Long id);

    /**
     * Retrieves a user by email address.
     * Used primarily for authentication and duplicate email checks.
     *
     * @param email the email to search for
     * @return UserResponse if found
     * @throws UserException if user not found
     */
    UserResponse getUserByEmail(String email);

    /**
     * Creates a new user in the system.
     * ADMIN only access.
     *
     * Business Logic:
     * - Validates input using @Valid
     * - Checks for duplicate email
     * - Encrypts password using BCrypt
     * - Sets createdAt timestamp
     * - Saves to database
     * - Returns created user without password
     *
     * @param userRequest the user creation request containing name, email, password, role, status
     * @return UserResponse of created user (without password)
     * @throws UserException if validation fails or email already exists
     */
    UserResponse createUser(UserRequest userRequest);

    /**
     * Updates an existing user's details.
     * ADMIN can update any user; users can update only their own profile.
     *
     * Business Logic:
     * - Validates input
     * - Prevents role escalation unless by ADMIN
     * - Checks for duplicate email if email is changed
     * - Updates only provided fields (partial update)
     * - Sets updatedAt timestamp
     * - Does NOT update password via this method (separate endpoint)
     *
     * @param id the user ID to update
     * @param userRequest the update request with new values
     * @return UserResponse of updated user
     * @throws UserException if user not found, validation fails, or access denied
     */
    UserResponse updateUser(Long id, UserRequest userRequest);

    /**
     * Updates a user's account status (activate/deactivate).
     * ADMIN only access.
     *
     * Business Logic:
     * - Updates only status field
     * - Sets updatedAt timestamp
     * - Validates status transitions if needed
     *
     * @param id the user ID to update
     * @param status the new status (ACTIVE or INACTIVE)
     * @return UserResponse of user with updated status
     * @throws UserException if user not found
     */
    UserResponse updateUserStatus(Long id, UserStatusEnum status);

    /**
     * Deletes a user from the system.
     * ADMIN only access.
     * Soft delete recommended in production (not implemented here).
     *
     * @param id the user ID to delete
     * @throws UserException if user not found
     */
    void deleteUser(Long id);

    /**
     * Checks if email is already registered in the system.
     * Used to prevent duplicate registrations.
     *
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean emailExists(String email);

    /**
     * Validates that a user is active before allowing login/access.
     * Used in security checks.
     *
     * @param id the user ID to validate
     * @return true if user exists and is ACTIVE, false otherwise
     */
    boolean isUserActive(Long id);
}

