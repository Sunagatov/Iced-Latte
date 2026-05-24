package com.zufar.icedlatte.user.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SingleUserProvider implements UserLookupApi, UserAuthenticationApi {

    private final UserRepository userCrudRepository;
    private final UserDtoConverter userDtoConverter;

    @Override
    @Transactional(readOnly = true)
    public UserLookupSnapshot getUserById(final UUID userId) throws UserNotFoundException {
        return toLookupSnapshot(getUserEntityById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserLookupSnapshot getUserByEmail(final String email) throws UserNotFoundException {
        return toLookupSnapshot(getUserEntityByEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationSnapshot> findUserAuthenticationByEmail(final String email) {
        return userCrudRepository.findByEmail(email).map(this::toAuthenticationSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationSnapshot> findUserAuthenticationById(final UUID userId) {
        return userCrudRepository.findById(userId).map(this::toAuthenticationSnapshot);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(final UUID userId) throws UserNotFoundException {
        return userCrudRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityByEmail(final String email) throws UserNotFoundException {
        return userCrudRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    public UserDto getUserDtoById(final UUID userId) throws UserNotFoundException {
        return userDtoConverter.toDto(getUserEntityById(userId));
    }

    private UserLookupSnapshot toLookupSnapshot(UserEntity user) {
        return new UserLookupSnapshot(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }

    private UserAuthenticationSnapshot toAuthenticationSnapshot(UserEntity user) {
        List<String> authorities = user.getAuthorities().stream()
                .map(UserGrantedAuthority::getAuthority)
                .toList();
        return new UserAuthenticationSnapshot(
                user.getId(),
                user.getEmail(),
                Objects.requireNonNull(user.getPassword()),
                authorities,
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled());
    }
}
