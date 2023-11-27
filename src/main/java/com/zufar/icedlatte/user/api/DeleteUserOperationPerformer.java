package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteUserOperationPerformer {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteUser(final UUID userId) throws InvalidOldPasswordException {
        userRepository.deleteById(userId);
    }
}
