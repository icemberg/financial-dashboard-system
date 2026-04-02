package com.financedashboard.zorvyn.service.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for resolving user identity and access control.
 * Responsible for user lookup, role-based filtering, and authorization logic.
 * Follows Single Responsibility Principle by centralizing user resolution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserResolutionUtil {

    private final UserRepository userRepository;

    /**
     * Fetches user by email or throws FinancialDashboardException if not found.
     * Centralizes user lookup logic that was previously scattered.
     *
     * @param email the user's email address
     * @return the User entity if found
     * @throws FinancialDashboardException if user is not found
     */
    public User getUserOrThrow(String email) {
        log.debug("Resolving user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new FinancialDashboardException(
                            "User not found for email: " + email,
                            ErrorCodeEnum.USER_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    );
                });
    }

    /**
     * Resolves the userId filter based on user role.
     * ADMIN users get null (see all records), non-admin users get filtered by their ID.
     * Implements row-level security through this filter.
     *
     * @param user the User entity
     * @return userId if user is not ADMIN, null if user is ADMIN
     */
    public Long resolveUserIdFilter(User user) {
        Long userIdFilter = user.getRole() == RolesEnum.ADMIN ? null : user.getId();
        log.debug("Resolved user ID filter for user={} (role={}): userIdFilter={}", 
                  user.getId(), user.getRole(), userIdFilter);
        return userIdFilter;
    }

    /**
     * Convenience method: fetch user by email and resolve their userId filter in one call.
     * Combines user lookup and role-based filtering.
     *
     * @param email the user's email address
     * @return the userId filter (null for ADMIN, user ID for others)
     * @throws FinancialDashboardException if user is not found
     */
    public Long resolveUserIdFilterByEmail(String email) {
        User user = getUserOrThrow(email);
        return resolveUserIdFilter(user);
    }
}
