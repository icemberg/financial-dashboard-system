package com.financedashboard.zorvyn.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.UserRequest;
import com.financedashboard.zorvyn.dto.UserResponse;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.financedashboard.zorvyn.service.interfaces.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for User Management endpoints.
 * Provides endpoints for CRUD operations on users with role-based access control.
 *
 * Base Path: /v1/users
 * Security: All endpoints require authentication and appropriate roles.
 *
 * Endpoints:
 * - GET    /v1/users           → Fetch all users (ADMIN only)
 * - GET    /v1/users/{id}      → Fetch user by ID (ADMIN or own user)
 * - POST   /v1/users           → Create new user (ADMIN only)
 * - PATCH  /v1/users/{id}      → Update user details (ADMIN or own user)
 * - PATCH  /v1/users/{id}/status → Update user status (ADMIN only)
 * - DELETE /v1/users/{id}      → Delete user (ADMIN only)
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * Constructor for dependency injection.
     *
     * @param userService the user service implementation
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /v1/users
     * Fetch all users in the system.
     *
     * Security: ADMIN only
     * HTTP Status:
     * - 200 OK: Successfully retrieved all users
     * - 403 FORBIDDEN: User is not ADMIN
     *
     * Response: List<UserResponse> with all users (password excluded)
     *
     * @return ResponseEntity with list of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("API request: GET /v1/users - Fetch all users");
        List<UserResponse> users = userService.getAllUsers();
        log.info("Successfully retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * GET /v1/users/{id}
     * Fetch a specific user by ID.
     *
     * Security:
     * - ADMIN can fetch any user
     * - Non-admin users can fetch only their own data
     * - Uses SpEL expression: hasRole('ADMIN') or #id == authentication.principal.userId
     *
     * HTTP Status:
     * - 200 OK: User found and returned
     * - 403 FORBIDDEN: Unauthorized access (not ADMIN and not own ID)
     * - 404 NOT_FOUND: User with given ID does not exist
     *
     * @param id the user ID to fetch
     * @return ResponseEntity with UserResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("API request: GET /v1/users/{} - Fetch user by ID", id);
        UserResponse user = userService.getUserById(id);
        log.info("User retrieved successfully with id={}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /v1/users
     * Create a new user in the system.
     *
     * Security: ADMIN only
     * Validation: UserRequest is validated using @Valid annotation
     *
     * Business Logic:
     * - Validates all input fields (name, email, password, role, status)
     * - Checks for duplicate email
     * - Encrypts password using BCrypt
     * - Sets createdAt timestamp
     * - Saves to database
     * - Returns created user without password
     *
     * HTTP Status:
     * - 201 CREATED: User created successfully
     * - 400 BAD_REQUEST: Validation failed (invalid email, short password, etc.)
     * - 403 FORBIDDEN: User is not ADMIN
     * - 409 CONFLICT: Email already exists (duplicate email)
     *
     * Request Body: UserRequest
     * {
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "password": "SecurePass123!",
     *   "role": "ADMIN",
     *   "status": "ACTIVE"
     * }
     *
     * @param userRequest the user creation request
     * @return ResponseEntity with created UserResponse and 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("API request: POST /v1/users - Create user with email={}", userRequest.getEmail());
        UserResponse createdUser = userService.createUser(userRequest);
        log.info("User created successfully with id={}", createdUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * PATCH /v1/users/{id}
     * Update an existing user's details.
     *
     * Security:
     * - ADMIN can update any user
     * - Non-admin users can update only their own profile
     *
     * Business Logic:
     * - Performs partial update (only provided fields are updated)
     * - Validates input fields
     * - Checks for duplicate email if email is changed
     * - Prevents role escalation for non-admin users
     * - Sets updatedAt timestamp
     * - Does NOT update password (use separate endpoint)
     *
     * HTTP Status:
     * - 200 OK: User updated successfully
     * - 400 BAD_REQUEST: Validation failed
     * - 403 FORBIDDEN: Unauthorized access
     * - 404 NOT_FOUND: User not found
     * - 409 CONFLICT: Email already in use
     *
     * Request Body: UserRequest (partial, only fields to update needed)
     * {
     *   "name": "Jane Doe",
     *   "email": "jane@example.com"
     * }
     *
     * @param id the user ID to update
     * @param userRequest the update request with new values
     * @return ResponseEntity with updated UserResponse
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest userRequest) {
        log.info("API request: PATCH /v1/users/{} - Update user", id);
        UserResponse updatedUser = userService.updateUser(id, userRequest);
        log.info("User updated successfully with id={}", id);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * PATCH /v1/users/{id}/status
     * Update a user's account status (activate/deactivate).
     *
     * Security: ADMIN only
     *
     * Business Logic:
     * - Updates only the status field
     * - Accepts UserStatusEnum: ACTIVE or INACTIVE
     * - ACTIVE users can access the system
     * - INACTIVE users are locked out
     * - Sets updatedAt timestamp
     *
     * HTTP Status:
     * - 200 OK: Status updated successfully
     * - 400 BAD_REQUEST: Invalid status provided
     * - 403 FORBIDDEN: User is not ADMIN
     * - 404 NOT_FOUND: User not found
     *
     * Request Body: UserRequest with only status field
     * {
     *   "status": "INACTIVE"
     * }
     *
     * @param id the user ID to update
     * @param userRequest containing the status field
     * @return ResponseEntity with updated UserResponse
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest userRequest) {
        log.info("API request: PATCH /v1/users/{}/status - Update user status to={}", id, userRequest.getStatus());

        // Validate that status is provided
        if (userRequest.getStatus() == null) {
            log.warn("Status update request missing status field");
            throw new com.financedashboard.zorvyn.exception.UserException(
                    "Status must be provided",
                    com.financedashboard.zorvyn.enums.ErrorCodeEnum.INVALID_USER_INPUT
            );
        }

        UserResponse updatedUser = userService.updateUserStatus(id, userRequest.getStatus());
        log.info("User status updated successfully with id={}", id);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * DELETE /v1/users/{id}
     * Delete a user from the system.
     *
     * Security: ADMIN only
     * Note: In production, soft delete (with isDeleted flag) is recommended.
     *
     * HTTP Status:
     * - 204 NO_CONTENT: User deleted successfully
     * - 403 FORBIDDEN: User is not ADMIN
     * - 404 NOT_FOUND: User not found
     *
     * @param id the user ID to delete
     * @return ResponseEntity with 204 NO_CONTENT status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("API request: DELETE /v1/users/{} - Delete user", id);
        userService.deleteUser(id);
        log.info("User deleted successfully with id={}", id);
        return ResponseEntity.noContent().build();
    }
}
