package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import com.zufar.onlinestore.user.util.ConfirmUserEmailTokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${iced-latte.feature.email-confirmation.token.pattern}")
    private String tokenPattern;

    @Value("${iced-latte.feature.email-confirmation.token.pattern-placeholder-char}")
    private String patternPlaceholder;
    private final ConfirmUserEmailTokenGenerator confirmUserEmailTokenGenerator = new ConfirmUserEmailTokenGenerator();


    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public String generateUserEmailConfirmationToken(final UUID userId) {
        var userEntity = userCrudRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to get the user with the id = {}.", userId);
                    return new UserNotFoundException(userId);
                });
        final var token = confirmUserEmailTokenGenerator.nextToken(
                ConfirmUserEmailTokenGenerator.DEFAULT_BASE,
                tokenPattern,
                patternPlaceholder.charAt(0)
        );
        userEntity.setConfirmationToken(token);
        userCrudRepository.save(userEntity);
        return token;
    }

    public void confirmUserEmail(final String token) {
        userCrudRepository.findByConfirmationToken(token).ifPresent(
                user -> {
                    user.setEmailConfirmed(true);
                    user.setConfirmationToken(null);
                    userCrudRepository.save(user);
                }
        );
    }
}
