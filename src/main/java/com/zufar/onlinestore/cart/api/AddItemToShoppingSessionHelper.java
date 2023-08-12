package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddItemToShoppingSessionHelper {

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ProductInfoRepository productInfoRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductApi productApi;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final ShoppingSessionCreator shoppingSessionCreator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto add(final List<NewShoppingSessionItemDto> items) {
//        UserDto userDto = securityPrincipalProvider.get();
//        UUID userId = userDto.userId();
//
//        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(userId);
//        if (shoppingSession == null) {
//            shoppingSession = shoppingSessionCreator.create();
//        }
//
//        List<ShoppingSessionItem> newItems = convert(items);
//
//        shoppingSession.setItems(newItems);

        throw new UnsupportedOperationException();
    }

    private ShoppingSessionDto getShoppingSessionByUserId(UUID userId) {
        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(userId);
        if (shoppingSession == null) {
            log.error("The shopping session for the user with id = {} is not found.", userId);
            throw new ShoppingSessionNotFoundException(userId);
        }

        try {
            return shoppingSessionProvider.getByUserId(userId);
        } catch (ShoppingSessionNotFoundException exception) {
            return null;
        }
    }
}
