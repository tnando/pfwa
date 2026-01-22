package com.pfwa.security;

import com.pfwa.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * UserDetails implementation for Spring Security authentication.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean emailVerified;
    private final boolean accountLocked;
    private final int tokenVersion;
    private final UUID sessionId;

    public UserPrincipal(User user, UUID sessionId) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.emailVerified = user.isEmailVerified();
        this.accountLocked = user.isAccountLocked();
        this.tokenVersion = user.getTokenVersion();
        this.sessionId = sessionId;
    }

    public static UserPrincipal create(User user, UUID sessionId) {
        return new UserPrincipal(user, sessionId);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}
