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

    public UserDto get() {
        UserDtoConverter userDtoConverter = new UserDtoConverter();
        UserEntity userEntity = (UserEntity) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return userDtoConverter.toDto(userEntity);
    }

    public UUID getUserId() {
        return get().getId();
    }
}
