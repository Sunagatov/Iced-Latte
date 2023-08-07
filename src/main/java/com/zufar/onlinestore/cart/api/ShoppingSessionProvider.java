package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
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
    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ShoppingSessionDto getByUserId(final UUID userId) throws ShoppingSessionNotFoundException {
        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(userId);
        if (shoppingSession == null) {
            log.error("The shopping session for the user with id = {} is not found.", userId);
            throw new ShoppingSessionNotFoundException(userId);
        }
        return shoppingSessionDtoConverter.toDto(shoppingSession); //ToDo think about return type
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest request) {
        Integer deletedItems = shoppingSessionItemRepository.deleteShoppingSessionItemById(request.shoppingSessionItemId());
        if (deletedItems < request.shoppingSessionItemId().size()) {
            log.error("The list of shopping session items with ids: {} does not exist.", request.shoppingSessionItemId());
            throw new ShoppingSessionItemNotFoundException(request.shoppingSessionItemId());
        }
        //ToDo update list before making an exception, show only invalid ids

        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(request.userId());
        if (shoppingSession == null) {
            log.error("The shopping session for the user with id = {} is not found.", request.userId());
            throw new ShoppingSessionNotFoundException(request.userId());
        }
        return shoppingSessionDtoConverter.toDto(shoppingSession);
    }
}