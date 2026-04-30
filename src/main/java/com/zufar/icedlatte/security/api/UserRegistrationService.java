package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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

    private final UserRepository userRepository;
    private final RegistrationDtoConverter registrationDtoConverter;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    @Transactional
    public UserAuthenticationResponse register(final UserRegistrationRequest userRegistrationRequest,
                                               final HttpServletRequest httpRequest) {
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
            UserEntity userEntity = userRepository.saveAndFlush(newUserEntity);
            log.info("auth.registration.succeeded: userId={}", userEntity.getId());
            return sessionTokenService.issueForNewSession(userEntity, httpRequest);
        } catch (DataIntegrityViolationException e) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw new UserRegistrationException("Registration failed.", e);
        }
    }
}
