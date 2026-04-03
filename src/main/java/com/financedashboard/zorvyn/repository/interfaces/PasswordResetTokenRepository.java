package com.financedashboard.zorvyn.repository.interfaces;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.financedashboard.zorvyn.entity.PasswordResetToken;
import com.financedashboard.zorvyn.entity.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds an unused (not yet consumed) token by its string value.
     * Used by the reset-password flow to validate the incoming token.
     */
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    /**
     * Deletes all unused tokens belonging to a user before issuing a new one.
     * Prevents token accumulation and ensures only the latest reset link is valid.
     *
     * @Modifying required because this is a delete operation in a @Transactional context.
     */
    @Modifying
    void deleteByUserAndUsedFalse(User user);
}
