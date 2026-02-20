package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationResponse;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userCrudRepository;
    private final RegistrationDtoConverter registrationDtoConverter;
    private final PasswordEncoder passwordEncoder;

    public boolean isEmailAvailable(final String email) {
        return userCrudRepository.findByEmail(email).isEmpty();
    }

    @Transactional
    public UserRegistrationResponse register(final UserRegistrationRequest userRegistrationRequest) {
        String email = userRegistrationRequest.getEmail().toLowerCase(java.util.Locale.ROOT).trim();
        String encryptedPassword = passwordEncoder.encode(userRegistrationRequest.getPassword());
        UserGrantedAuthority defaultUserGrantedAuthority = UserGrantedAuthority.builder().authority(Authority.USER).build();

        UserEntity newUserEntity = registrationDtoConverter.toEntity(userRegistrationRequest);
        newUserEntity.setEmail(email);
        newUserEntity.setPassword(encryptedPassword);
        newUserEntity.addAuthority(defaultUserGrantedAuthority);
        newUserEntity.setAccountNonExpired(true);
        newUserEntity.setAccountNonLocked(true);
        newUserEntity.setCredentialsNonExpired(true);
        newUserEntity.setEnabled(true);

        try {
            UserEntity userEntity = userCrudRepository.saveAndFlush(newUserEntity);
            final String jwtToken = jwtTokenProvider.generateToken(userEntity);
            final String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userEntity);
            UserRegistrationResponse response = new UserRegistrationResponse();
            response.setToken(jwtToken);
            response.setRefreshToken(jwtRefreshToken);
            return response;
        } catch (DataIntegrityViolationException e) {
            throw new UserRegistrationException(email, "User with email = '" + email + "' is already registered.");
        }
    }
}
