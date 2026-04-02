package com.financedashboard.zorvyn.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.UserStatusEnum;

/**
 * Wraps the User entity into Spring Security's UserDetails contract.
 * Authority format: ROLE_ADMIN, ROLE_ANALYST, ROLE_VIEWER — required by @PreAuthorize("hasRole(...)").
 * Google users (null password) return empty string to prevent NPE in Spring Security internals.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** Returns empty string for Google-authenticated users who have no local password. */
    @Override
    public String getPassword() {
        return user.getPassword() != null ? user.getPassword() : "";
    }

    /** Spring Security uses email as the username throughout this application. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == UserStatusEnum.ACTIVE;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatusEnum.ACTIVE;
    }

    /** Exposes the user's database ID for service-level ownership checks. */
    public Long getUserId() {
        return user.getId();
    }

    /** Provides direct access to the User entity when full entity data is needed. */
    public User getUser() {
        return user;
    }
}
