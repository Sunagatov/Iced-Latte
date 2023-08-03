package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFromSecurityContextProvider {

    private final UserDtoConverter userDtoConverter;

    public UserDto getUser() {
        UserEntity userEntity = (UserEntity) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return userDtoConverter.toDto(userEntity);
    }
}
