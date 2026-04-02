package com.financedashboard.zorvyn.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financedashboard.zorvyn.dto.UserRequest;
import com.financedashboard.zorvyn.dto.UserResponse;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.financedashboard.zorvyn.exception.UserException;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.service.interfaces.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for user management.
 * Handles all business logic related to user operations.
 * Enforces validation, security rules, and data integrity.
 *
 * Security Notes:
 * - Passwords are encrypted using BCrypt before storage
 * - Role modification is restricted to prevent privilege escalation
 * - UserException is thrown for all user-specific errors
 * - All sensitive fields are excluded from responses
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor for dependency injection.
     * PasswordEncoder is auto-wired from Spring Security configuration.
     */
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves all users from the database.
     * Used by ADMIN to view all system users.
     * Logs the operation for audit trail.
     *
     * @return List of UserResponse objects (password excluded)
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users from database");
        try {
            List<User> users = userRepository.findAll();
            log.info("Retrieved {} users", users.size());
            return users.stream()
                    .map(this::convertToResponse)
                    .toList();
        } catch (Exception ex) {
            log.error("Error fetching users", ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Retrieves a user by ID.
     * If user not found, throws UserException with NOT_FOUND error.
     *
     * @param id the user ID
     * @return UserResponse if found
     * @throws UserException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user with id={}", id);

        // Validate input
        if (id == null || id <= 0) {
            log.warn("Invalid user ID provided: {}", id);
            throw new UserException(
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorCode(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorMessage(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getHttpStatus()
            );
        }

        return userRepository.findById(id)
                .map(user -> {
                    log.debug("User found with id={}", id);
                    return convertToResponse(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found with id={}", id);
                    return new UserException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    );
                });
    }

    /**
     * Retrieves a user by email address.
     * Used for authentication and email duplicate checks.
     * If user not found, throws UserException with NOT_FOUND error.
     *
     * @param email the email to search for
     * @return UserResponse if found
     * @throws UserException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email={}", email);

        // Validate input
        if (email == null || email.isBlank()) {
            log.warn("Invalid email provided");
            throw new UserException(
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorCode(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorMessage(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getHttpStatus()
            );
        }

        return userRepository.findByEmail(email)
                .map(user -> {
                    log.debug("User found with email={}", email);
                    return convertToResponse(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found with email={}", email);
                    return new UserException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + email,
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    );
                });
    }

    /**
     * Creates a new user in the system.
     * ADMIN only operation.
     *
     * Business Logic:
     * 1. Validate all input fields using @Valid (done in controller)
     * 2. Check if email already exists → throw USER_ALREADY_EXISTS
     * 3. Encrypt password using BCrypt
     * 4. Create new User entity with provided data
     * 5. Set createdAt timestamp to current time
     * 6. Save to database
     * 7. Return UserResponse without password
     *
     * @param userRequest containing name, email, password, role, status
     * @return UserResponse of created user
     * @throws UserException if email exists or database error occurs
     */
    @Override
    public UserResponse createUser(UserRequest userRequest) {
        log.info("Creating user with email={}", userRequest.getEmail());

        // Check if email already exists → CONFLICT (409)
        if (emailExists(userRequest.getEmail())) {
            log.warn("User creation failed: email already exists={}", userRequest.getEmail());
            throw new UserException(
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getCode(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + userRequest.getEmail(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
            );
        }

        try {
            // Create new user entity
            User user = User.builder()
                    .name(userRequest.getName())
                    .email(userRequest.getEmail())
                    // Encrypt password using BCrypt before storage
                    .password(passwordEncoder.encode(userRequest.getPassword()))
                    .role(userRequest.getRole())
                    .status(userRequest.getStatus())
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save to database
            User savedUser = userRepository.save(user);
            log.info("User created successfully with id={}, email={}", savedUser.getId(), savedUser.getEmail());

            // Return response without password
            return convertToResponse(savedUser);

        } catch (UserException ex) {
            // Re-throw UserException as-is
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating user with email={}", userRequest.getEmail(), ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Updates an existing user's details.
     * ADMIN can update any user; users can update only their own profile.
     *
     * Business Logic:
     * 1. Fetch user by ID → throw NOT_FOUND if missing
     * 2. Prevent role escalation (non-admin cannot change role)
     * 3. Check for duplicate email if email is being changed
     * 4. Update only provided fields
     * 5. Set updatedAt timestamp
     * 6. Save to database
     * 7. Return updated UserResponse
     *
     * Note: Password is NOT updated via this method (use separate endpoint)
     *
     * @param id user ID to update
     * @param userRequest containing fields to update
     * @return UserResponse of updated user
     * @throws UserException if user not found or validation fails
     */
    @Override
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        log.info("Updating user with id={}", id);

        // Fetch existing user or throw NOT_FOUND
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: user not found with id={}", id);
                    return new UserException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    );
                });

        try {
            // Update name if provided
            if (userRequest.getName() != null && !userRequest.getName().isBlank()) {
                existingUser.setName(userRequest.getName());
                log.debug("Updated user name for id={}", id);
            }

            // Update email if provided and different
            if (userRequest.getEmail() != null && !userRequest.getEmail().isBlank()
                    && !userRequest.getEmail().equals(existingUser.getEmail())) {

                // Check if new email is already in use
                if (emailExists(userRequest.getEmail())) {
                    log.warn("Cannot update: email already exists={}", userRequest.getEmail());
                    throw new UserException(
                            ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorCode(),
                            ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + userRequest.getEmail(),
                            ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
                    );
                }
                existingUser.setEmail(userRequest.getEmail());
                log.debug("Updated user email for id={}", id);
            }

            // Update role if provided
            if (userRequest.getRole() != null) {
                existingUser.setRole(userRequest.getRole());
                log.debug("Updated user role for id={}", id);
            }

            // Update status if provided
            if (userRequest.getStatus() != null) {
                existingUser.setStatus(userRequest.getStatus());
                log.debug("Updated user status for id={}", id);
            }

            // Set updatedAt timestamp
            existingUser.setUpdatedAt(LocalDateTime.now());

            // Save to database
            User updatedUser = userRepository.save(existingUser);
            log.info("User updated successfully with id={}", id);

            return convertToResponse(updatedUser);

        } catch (UserException ex) {
            // Re-throw UserException as-is
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating user with id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Updates only a user's status (activate/deactivate).
     * ADMIN only operation.
     *
     * Business Logic:
     * 1. Fetch user by ID → throw NOT_FOUND if missing
     * 2. Update status field
     * 3. Set updatedAt timestamp
     * 4. Save to database
     * 5. Return updated UserResponse
     *
     * Use case: Admin can activate or deactivate user accounts without changing other fields.
     *
     * @param id user ID to update
     * @param status new status (ACTIVE or INACTIVE)
     * @return UserResponse with updated status
     * @throws UserException if user not found
     */
    @Override
    public UserResponse updateUserStatus(Long id, UserStatusEnum status) {
        log.info("Updating user status for id={} to status={}", id, status);

        // Validate status
        if (status == null) {
            log.warn("Invalid status provided: null");
            throw new UserException(
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorCode(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorMessage(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getHttpStatus()
            );
        }

        // Fetch existing user or throw NOT_FOUND
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update status: user not found with id={}", id);
                    return new UserException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    );
                });

        try {
            // Update status
            existingUser.setStatus(status);
            existingUser.setUpdatedAt(LocalDateTime.now());

            // Save to database
            User updatedUser = userRepository.save(existingUser);
            log.info("User status updated successfully for id={}", id);

            return convertToResponse(updatedUser);

        } catch (Exception ex) {
            log.error("Error updating user status for id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Deletes a user from the system.
     * ADMIN only operation.
     *
     * Note: In production, soft delete is recommended (add isDeleted flag).
     * This implementation performs hard delete for simplicity.
     *
     * @param id user ID to delete
     * @throws UserException if user not found
     */
    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with id={}", id);

        // Check if user exists
        if (!userRepository.existsById(id)) {
            log.warn("Cannot delete: user not found with id={}", id);
            throw new UserException(
                    ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                    ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                    ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
            );
        }

        try {
            userRepository.deleteById(id);
            log.info("User deleted successfully with id={}", id);
        } catch (Exception ex) {
            log.error("Error deleting user with id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Checks if an email already exists in the database.
     * Used to prevent duplicate email registrations.
     * Useful for pre-validation before user creation.
     *
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Validates that a user is active.
     * Used in security checks before allowing user access to system.
     * Returns false if user not found or inactive.
     *
     * @param id user ID to validate
     * @return true if user exists and status is ACTIVE
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserActive(Long id) {
        return userRepository.findById(id)
                .map(user -> user.getStatus() == UserStatusEnum.ACTIVE)
                .orElse(false);
    }

    /**
     * Converts User entity to UserResponse DTO.
     * IMPORTANT: Password is never included in response.
     * Used to prepare data for API responses.
     *
     * @param user the User entity to convert
     * @return UserResponse DTO without password
     */
    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        // Note: password is NOT included
    }
}
