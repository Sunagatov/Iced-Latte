package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingSessionCreator {

    private static final int DEFAULT_PRODUCTS_QUANTITY_WHEN_NEW_ITEM_IS_CREATED = 0;
    private static final int DEFAULT_ITEMS_QUANTITY_WHEN_NEW_ITEM_IS_CREATED = 0;

    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ShoppingSessionDto create() throws ShoppingSessionNotFoundException {
        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.userId();

        ShoppingSession newShoppingSession = ShoppingSession.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY_WHEN_NEW_ITEM_IS_CREATED)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY_WHEN_NEW_ITEM_IS_CREATED)
                .items(Collections.emptySet())
                .createdAt(LocalDateTime.now())
                .build();
        return shoppingSessionDtoConverter.toDto(newShoppingSession);
    }
}