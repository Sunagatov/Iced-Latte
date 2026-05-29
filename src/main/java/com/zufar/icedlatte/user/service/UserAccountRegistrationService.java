package com.zufar.icedlatte.user.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountRegistrationService implements UserRegistrationApi {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(
                Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"));
    }

    @Override
    @Transactional
    public UserAuthenticationSnapshot registerPasswordUser(
            String firstName, String lastName, String email, String encodedPassword) {
        return registerUser(firstName, lastName, email, encodedPassword, false);
    }

    @Override
    @Transactional
    public UserAuthenticationSnapshot registerOAuthUser(
            String firstName, String lastName, String email, String encodedPassword) {
        return registerUser(firstName, lastName, email, encodedPassword, true);
    }

    private UserAuthenticationSnapshot registerUser(
            String firstName, String lastName, String email, String encodedPassword, boolean oauthUser) {
        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"))
                .password(encodedPassword)
                .oauthUser(oauthUser)
                .build();
        return saveDefaultEnabledUser(user);
    }

    private UserAuthenticationSnapshot saveDefaultEnabledUser(UserEntity user) {
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user.addAuthority(
                UserGrantedAuthority.builder().authority(Authority.USER).build());

        UserEntity saved = userRepository.saveAndFlush(user);
        return new UserAuthenticationSnapshot(
                saved.getId(),
                saved.getEmail(),
                Objects.requireNonNull(saved.getPassword()),
                List.of(Authority.USER.name()),
                saved.isAccountNonExpired(),
                saved.isAccountNonLocked(),
                saved.isCredentialsNonExpired(),
                saved.isEnabled());
    }
}
