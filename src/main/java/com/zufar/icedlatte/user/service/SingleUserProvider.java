package com.zufar.icedlatte.user.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SingleUserProvider implements UserLookupApi, UserAuthenticationApi {

    private final UserRepository userCrudRepository;

    @Override
    @Transactional(readOnly = true)
    public UserLookupSnapshot getUserById(final UUID userId) throws UserNotFoundException {
        return toLookupSnapshot(getUserEntityById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UserLookupSnapshot> getUsersByIds(final Set<UUID> userIds) {
        Objects.requireNonNull(userIds, "userIds must not be null");
        return userCrudRepository.findAllById(userIds).stream()
                .map(this::toLookupSnapshot)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public UserLookupSnapshot getUserByEmail(final String email) throws UserNotFoundException {
        return toLookupSnapshot(getUserEntityByEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLookupSnapshot> findUserByEmail(final String email) {
        return userCrudRepository
                .findByEmail(Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"))
                .map(this::toLookupSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationSnapshot> findUserAuthenticationByEmail(final String email) {
        return userCrudRepository
                .findByEmailWithAuthorities(
                        Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"))
                .map(this::toAuthenticationSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationSnapshot> findUserAuthenticationById(final UUID userId) {
        return userCrudRepository.findByIdWithAuthorities(userId).map(this::toAuthenticationSnapshot);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(final UUID userId) throws UserNotFoundException {
        return userCrudRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserEntity getUserEntityByEmail(final String email) throws UserNotFoundException {
        String normalizedEmail = Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null");
        return userCrudRepository.findByEmail(normalizedEmail).orElseThrow(() -> new UserNotFoundException(email));
    }

    private UserLookupSnapshot toLookupSnapshot(UserEntity user) {
        return new UserLookupSnapshot(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }

    private UserAuthenticationSnapshot toAuthenticationSnapshot(UserEntity user) {
        List<String> authorities =
                Objects.requireNonNull(user.getAuthorities(), "user authorities must not be null").stream()
                        .map(UserGrantedAuthority::getAuthority)
                        .map(authority -> Objects.requireNonNull(authority, "authority name must not be null"))
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
