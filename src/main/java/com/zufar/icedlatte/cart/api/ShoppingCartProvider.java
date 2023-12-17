package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
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
public class ShoppingCartProvider {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartDtoConverter shoppingCartDtoConverter;
    private final ShoppingCartCreator shoppingCartCreator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ShoppingCartDto getByUserId(final UUID userId) {
        ShoppingCart shoppingCart = shoppingCartRepository.findShoppingCartByUserId(userId);
        if (shoppingCart == null) {
            log.info("The shopping cart was not found.");
            shoppingCart = shoppingCartCreator.createNewShoppingCart(userId);
        }
        return shoppingCartDtoConverter.toDto(shoppingCart);
    }
}