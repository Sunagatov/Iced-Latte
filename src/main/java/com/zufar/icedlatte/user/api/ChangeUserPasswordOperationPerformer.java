package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeUserPasswordOperationPerformer {

    private final SingleUserProvider singleUserProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void changeUserPassword(final UUID userId,
                                   final ChangeUserPasswordRequest request) throws InvalidOldPasswordException {
        var userEntity = singleUserProvider.getUserEntityById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), userEntity.getPassword())) {
            throw new InvalidOldPasswordException();
        }
        userRepository.changeUserPassword(passwordEncoder.encode(request.getNewPassword()), userId);
        authSessionService.revokeAllForUser(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void changeUserPassword(final UUID userId, final String newPassword) {
        userRepository.changeUserPassword(passwordEncoder.encode(newPassword), userId);
        authSessionService.revokeAllForUser(userId);
    }
}
