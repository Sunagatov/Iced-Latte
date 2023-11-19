package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.cart.entity.ShoppingSession;
import com.zufar.icedlatte.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingSessionRepository;
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
public class ShoppingSessionProvider {

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ShoppingSessionDto getByUserId(final UUID userId) throws ShoppingSessionNotFoundException {
        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(userId);
        if (shoppingSession == null) {
            log.error("The shopping session for the user with id = {} is not found.", userId);
            throw new ShoppingSessionNotFoundException(userId);
        }
        return shoppingSessionDtoConverter.toDto(shoppingSession);
    }
}