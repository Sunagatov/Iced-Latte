package com.zufar.icedlatte.security.service.principal;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.UserLookupApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultSecurityPrincipalProvider implements SecurityPrincipalProvider {

    private final UserLookupApi userLookupApi;

    @Override
    public UserDto get() {
        return userLookupApi.getUserById(getUserId());
    }

    @Override
    public UUID getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Identifiable principal)) {
            throw new UnauthorizedException("Authentication required.");
        }
        return principal.getId();
    }
}
