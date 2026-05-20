package com.zufar.icedlatte.user.service;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SingleUserProvider implements UserLookupApi, UserAuthenticationApi {

    private final UserRepository userCrudRepository;
    private final UserDtoConverter userDtoConverter;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(final UUID userId) throws UserNotFoundException {
        return userDtoConverter.toDto(getUserEntityById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(final String email) throws UserNotFoundException {
        return userDtoConverter.toDto(getUserEntityByEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationSnapshot> findUserAuthenticationByEmail(final String email) {
        return userCrudRepository.findByEmail(email)
                .map(user -> {
                    List<String> authorities = user.getAuthorities().stream()
                            .map(UserGrantedAuthority::getAuthority)
                            .toList();
                    return new UserAuthenticationSnapshot(
                            user.getId(),
                            user.getEmail(),
                            user.getPassword(),
                            authorities,
                            user.isAccountNonExpired(),
                            user.isAccountNonLocked(),
                            user.isCredentialsNonExpired(),
                            user.isEnabled()
                    );
                });
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(final UUID userId) throws UserNotFoundException {
        return userCrudRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityByEmail(final String email) throws UserNotFoundException {
        return userCrudRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

}
