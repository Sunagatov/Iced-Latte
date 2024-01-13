package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityPrincipalProvider {

    private final UserDtoConverter userDtoConverter;

    public UserDto get() {
        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();
        assert auth instanceof UsernamePasswordAuthenticationToken :
                String.format("Wrong type of token %s. " +
                        "Check SecurityConstants.java, SpringSecurityConfiguration.java and JwtAuthenticationFilter.java", auth.toString());
        UserEntity userEntity = (UserEntity) auth.getPrincipal();
        return userDtoConverter.toDto(userEntity);
    }

    public UUID getUserId() {
        return get().getId();
    }
}
