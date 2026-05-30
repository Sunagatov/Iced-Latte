package com.zufar.icedlatte.security.principal;

import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.user.api.UserLookupApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultCurrentUserProvider implements CurrentUserProvider, CurrentUserIdProvider {

    private final UserLookupApi userLookupApi;

    @Override
    public @NonNull CurrentUserSnapshot get() {
        var user = userLookupApi.getUserById(getUserId());
        return new CurrentUserSnapshot(user.id(), user.email());
    }

    @Override
    public @NonNull UUID getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Identifiable principal)) {
            throw new UnauthorizedException("Authentication required.");
        }
        return principal.getId();
    }
}
