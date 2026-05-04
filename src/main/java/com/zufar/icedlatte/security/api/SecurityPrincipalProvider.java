package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityPrincipalProvider {

    private final UserDtoConverter userDtoConverter;

    public UserDto get() {
        return userDtoConverter.toDto(getAuthenticatedUser());
    }

    public UUID getUserId() {
        return getAuthenticatedUser().getId();
    }

    private UserEntity getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserEntity userEntity)) {
            throw new UnauthorizedException("Authentication required.");
        }
        return userEntity;
    }
}
