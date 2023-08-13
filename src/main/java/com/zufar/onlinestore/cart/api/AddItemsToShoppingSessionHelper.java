package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddItemsToShoppingSessionHelper {

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductInfoRepository productInfoRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto add(final List<NewShoppingSessionItemDto> items) {
        Map<UUID, Integer> productsWithQuantity = items
                .stream()
                .collect(Collectors.toMap(NewShoppingSessionItemDto::productId, NewShoppingSessionItemDto::productsQuantity));
        Set<UUID> productIds = productsWithQuantity.keySet();

        Set<ShoppingSessionItem> shoppingSessionItems = productInfoRepository
                .findAllById(productIds).stream()
                .map(productInfo -> ShoppingSessionItem.builder()
                        .productsQuantity(productsWithQuantity.get(productInfo.getProductId()))
                        .productInfo(productInfo)
                        .build())
                .collect(Collectors.toSet());

        UUID userId = securityPrincipalProvider.getUserId();
        int itemsQuantity = shoppingSessionItems.size();
        int productsQuantity = productIds.size();

        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(userId);
        if (shoppingSession == null) {
            shoppingSession = ShoppingSession.builder()
                    .userId(userId)
                    .itemsQuantity(itemsQuantity)
                    .productsQuantity(productsQuantity)
                    .items(shoppingSessionItems)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            shoppingSession.setItemsQuantity(shoppingSession.getItemsQuantity() + itemsQuantity);
            shoppingSession.setProductsQuantity(shoppingSession.getProductsQuantity() + productsQuantity);
        }

        ShoppingSession updatedShoppingSession = shoppingSessionRepository.save(shoppingSession);
        return shoppingSessionDtoConverter.toDto(updatedShoppingSession);
    }
}
