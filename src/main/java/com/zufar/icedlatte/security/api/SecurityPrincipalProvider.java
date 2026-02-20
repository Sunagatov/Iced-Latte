package com.zufar.icedlatte.security.api;

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
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserEntity userEntity)) {
            throw new IllegalStateException("No authenticated UserEntity in security context");
        }
        return userDtoConverter.toDto(userEntity);
    }

    public UUID getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserEntity userEntity)) {
            throw new IllegalStateException("No authenticated UserEntity in security context");
        }
        return userEntity.getId();
    }
}
