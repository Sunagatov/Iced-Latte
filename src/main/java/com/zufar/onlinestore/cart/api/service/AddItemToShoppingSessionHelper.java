package com.zufar.onlinestore.cart.api.service;

import com.zufar.onlinestore.cart.api.ShoppingSessionProvider;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
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
import java.util.Optional;
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

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto add(UUID productId) {
        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.userId();
        Optional<ShoppingSessionDto> shoppingSession = getShoppingSessionByUserId(userId);

        if (shoppingSession.isEmpty()) {
            ProductInfoDto productInfo = productApi.getProduct(productId);
            createNewShoppingSession(userId, productInfo);
        }

      throw new UnsupportedOperationException();
    }

    private Optional<ShoppingSessionDto> getShoppingSessionByUserId(UUID userId) {
        Optional<ShoppingSessionDto> shoppingSessionDto;
        try {
            shoppingSessionDto = Optional.ofNullable(shoppingSessionProvider.getByUserId(userId));
        } catch (Exception exception) {
            shoppingSessionDto = Optional.empty();
        }
        return shoppingSessionDto;
    }
}
