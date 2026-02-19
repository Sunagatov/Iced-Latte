package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeUserPasswordOperationPerformer {

    private final SingleUserProvider singleUserProvider;
    private final UserRepository userRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void changeUserPassword(final ChangeUserPasswordRequest changeUserPasswordRequest) throws InvalidOldPasswordException {
        UUID userId = securityPrincipalProvider.getUserId();
        var userEntity = singleUserProvider.getUserEntityById(userId);

        if (!passwordEncoder.matches(changeUserPasswordRequest.getOldPassword(), userEntity.getPassword())) {
            log.warn("User with userEmail = '{}' provided incorrect password.", userEntity.getEmail());
            throw new InvalidOldPasswordException(userEntity.getEmail());
        }

        userRepository.changeUserPassword(passwordEncoder.encode(changeUserPasswordRequest.getNewPassword()), userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void changeUserPassword(final UUID userId, final String newPassword) {
        String newEncryptedPassword = passwordEncoder.encode(newPassword);
        userRepository.changeUserPassword(newEncryptedPassword, userId);
    }
}
