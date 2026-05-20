package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserLookupApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityPrincipalProvider {

    private final UserLookupApi userLookupApi;

    public UserDto get() {
        return userLookupApi.getUserById(getUserId());
    }

    public UUID getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Identifiable principal)) {
            throw new UnauthorizedException("Authentication required.");
        }
        return principal.getId();
    }
}
