package com.financedashboard.zorvyn.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financedashboard.zorvyn.dto.UserRequest;
import com.financedashboard.zorvyn.dto.UserResponse;
import com.financedashboard.zorvyn.dto.UserUpdateRequest;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.financedashboard.zorvyn.exception.UserException;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.service.interfaces.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        try {
            List<User> users = userRepository.findAll();
            log.info("Retrieved {} users", users.size());
            return users.stream().map(this::convertToResponse).toList();
        } catch (Exception ex) {
            log.error("Error fetching all users", ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user id={}", id);

        if (id == null || id <= 0) {
            throw new UserException(
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorCode(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorMessage(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getHttpStatus()
            );
        }

        try {
            return userRepository.findById(id)
                    .map(this::convertToResponse)
                    .orElseThrow(() -> new UserException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    ));
        } catch (UserException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching user id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user email={}", email);

        if (email == null || email.isBlank()) {
            throw new UserException(
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorCode(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorMessage(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getHttpStatus()
            );
        }

        try {
            return userRepository.findByEmail(email)
                    .map(this::convertToResponse)
                    .orElseThrow(() -> new UserException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + email,
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    ));
        } catch (UserException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching user email={}", email, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    @Override
    public UserResponse createUser(UserRequest userRequest) {
        log.info("Creating user email={}", userRequest.getEmail());

        if (emailExists(userRequest.getEmail())) {
            throw new UserException(
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getCode(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + userRequest.getEmail(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
            );
        }

        try {
            User user = User.builder()
                    .name(userRequest.getName())
                    .email(userRequest.getEmail())
                    .password(passwordEncoder.encode(userRequest.getPassword()))
                    .role(userRequest.getRole())
                    .status(userRequest.getStatus())
                    .createdAt(LocalDateTime.now())
                    .build();

            User saved = userRepository.save(user);
            log.info("User created: id={}, email={}", saved.getId(), saved.getEmail());
            return convertToResponse(saved);

        } catch (UserException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating user email={}", userRequest.getEmail(), ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Partial update — only non-null/non-blank fields in UserUpdateRequest are applied.
     * Password is intentionally excluded; use PATCH /v1/auth/change-password instead.
     * Bug #1 fix: no longer requires all 5 fields for a single-field update.
     */
    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest) {
        log.info("Updating user id={}", id);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserException(
                        ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                        ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                        ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                ));

        try {
            if (updateRequest.getName() != null && !updateRequest.getName().isBlank()) {
                existingUser.setName(updateRequest.getName());
            }

            if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()
                    && !updateRequest.getEmail().equals(existingUser.getEmail())) {
                if (emailExists(updateRequest.getEmail())) {
                    throw new UserException(
                            ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorCode(),
                            ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + updateRequest.getEmail(),
                            ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
                    );
                }
                existingUser.setEmail(updateRequest.getEmail());
            }

            if (updateRequest.getRole() != null) {
                existingUser.setRole(updateRequest.getRole());
            }

            if (updateRequest.getStatus() != null) {
                existingUser.setStatus(updateRequest.getStatus());
            }

            existingUser.setUpdatedAt(LocalDateTime.now());
            User updated = userRepository.save(existingUser);
            log.info("User updated: id={}", id);
            return convertToResponse(updated);

        } catch (UserException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating user id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    @Override
    public UserResponse updateUserStatus(Long id, UserStatusEnum status) {
        log.info("Updating status for user id={} to {}", id, status);

        if (status == null) {
            throw new UserException(
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorCode(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getErrorMessage(),
                    ErrorCodeEnum.INVALID_USER_INPUT.getHttpStatus()
            );
        }

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserException(
                        ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                        ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                        ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                ));

        try {
            existingUser.setStatus(status);
            existingUser.setUpdatedAt(LocalDateTime.now());
            User updated = userRepository.save(existingUser);
            log.info("Status updated for user id={}", id);
            return convertToResponse(updated);
        } catch (Exception ex) {
            log.error("Error updating status for user id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user id={}", id);

        if (!userRepository.existsById(id)) {
            throw new UserException(
                    ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                    ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage() + ": " + id,
                    ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
            );
        }

        try {
            userRepository.deleteById(id);
            log.info("User deleted: id={}", id);
        } catch (Exception ex) {
            log.error("Error deleting user id={}", id, ex);
            throw new UserException(
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorCode(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DATA_ACCESS_ERROR.getHttpStatus()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) return false;
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserActive(Long id) {
        return userRepository.findById(id)
                .map(user -> user.getStatus() == UserStatusEnum.ACTIVE)
                .orElse(false);
    }

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
    }
}
