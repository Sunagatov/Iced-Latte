package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SingleUserProvider {

    private final UserRepository userCrudRepository;

    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(final UUID userId) throws UserNotFoundException {
        return userCrudRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityByEmail(final String email) throws UserNotFoundException {
        return userCrudRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

}
