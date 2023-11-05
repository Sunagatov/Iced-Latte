package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmUserEmailOperationPerformer {

    private final UserRepository userCrudRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public String generateUserEmailConfirmationToken(final UUID userId) {
        var userEntity = userCrudRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to get the user with the id = {}.", userId);
                    return new UserNotFoundException(userId);
                });
        final var token = generateConfirmationToken(userEntity.getId());
        userEntity.setConfirmationToken(token);
        userCrudRepository.save(userEntity);
        return token;
    }

    public void confirmUserEmail(final String token) {
        var userEntity = userCrudRepository.findByConfirmationToken(token)
                .orElseThrow(() -> {
                    log.error("Failed to get the user with the token = {}.", token);
                    return new UserNotFoundException(token);
                });
        userEntity.setEmailConfirmed(true);
        userEntity.setConfirmationToken(null);
        userCrudRepository.save(userEntity);
    }

    /**
     * Generates a user-unique confirmation token.
     * The token is a string of characters and digits based on user's UUID.
     *
     * @param userUuid the user's uuid.
     * @return the confirmation token.
     */
    private @NonNull String generateConfirmationToken(@NonNull UUID userUuid) {
        var remains = userUuid.hashCode();
        var charDigits = new StringBuilder();
        var radix = 97;
        while (remains != 0) {
            var charnum = Math.abs(remains % 100);
            if (charnum > 26) {
                charnum = charnum % 10;
                if (Math.abs(remains / 10) < 10 && remains < 0) {
                    radix = 65;
                }
                remains = remains / 10;
            } else {
                if (Math.abs(remains / 100) < 10 && remains < 0) {
                    radix = 65;
                }
                remains = remains / 100;
            }
            charDigits.append((char) (charnum + radix));
        }
        return charDigits.toString();
    }
}
