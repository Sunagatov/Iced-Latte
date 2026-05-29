package com.zufar.icedlatte.security.signin.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;

public record SecurityUserDetails(
        UUID id,
        String email,
        String password,
        List<GrantedAuthority> authorities,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        boolean enabled)
        implements UserDetails, Identifiable {

    public static SecurityUserDetails from(UserAuthenticationSnapshot snapshot) {
        List<GrantedAuthority> authorities = snapshot.authorities().stream()
                .map(SecurityUserDetails::roleAuthority)
                .map(GrantedAuthority.class::cast)
                .toList();
        return new SecurityUserDetails(
                snapshot.userId(),
                snapshot.email(),
                snapshot.encodedPassword(),
                authorities,
                snapshot.accountNonExpired(),
                snapshot.accountNonLocked(),
                snapshot.credentialsNonExpired(),
                snapshot.enabled());
    }

    private static SimpleGrantedAuthority roleAuthority(String authority) {
        return new SimpleGrantedAuthority(authority.startsWith("ROLE_") ? authority : "ROLE_" + authority);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    @Override
    @NonNull
    public String getPassword() {
        return password;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
