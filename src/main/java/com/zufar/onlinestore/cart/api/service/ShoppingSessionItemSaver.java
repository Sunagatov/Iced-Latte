package com.zufar.onlinestore.cart.api.service;

import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingSessionItemSaver {

    private final ShoppingSessionRepository shoppingSessionRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto save(final UUID shoppingSessionId,
                                   final UUID shoppingSessionItemId,
                                   final UUID productId) {
        Optional<ShoppingSession> shoppingSession = shoppingSessionRepository.findById(shoppingSessionId);
        if (shoppingSession.isEmpty()) {
            log.warn("Failed to add new item to the shoppingSession with the id = {}.", shoppingSessionId);
            throw new ShoppingSessionNotFoundException(shoppingSessionId);
        }

        throw new UnsupportedOperationException();
    }
}
