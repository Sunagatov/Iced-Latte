package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartApiImpl implements CartApi {
    
    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Override
    public ShoppingSessionDto getShoppingSession(final GetShoppingSessionRequest getShoppingSessionRequest) {
        return null;
    }

    @Override
    public ShoppingSessionDto addNewItemToShoppingSession(final AddNewItemToShoppingSessionRequest addNewItemToShoppingSessionRequest) {
        return null;
    }

    @Override
    public ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest removeItemFromShoppingSessionRequest) {
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto updateProductAmountInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest request) {
        ShoppingSession shoppingSession = shoppingSessionRepository.findById(request.shoppingSessionId())
                .orElseThrow();

        ShoppingSessionItem shoppingSessionItem = shoppingSession.getItems().stream()
                .filter(item -> Objects.equals(item.getId(), request.shoppingSessionItemId()))
                .findFirst()
                .orElseThrow();

        Integer newProductsQuantity = shoppingSessionItem.getProductsQuantity() + request.productsQuantityChange();
        shoppingSessionItem.setProductsQuantity(newProductsQuantity);

        shoppingSessionRepository.save(shoppingSession);

        return shoppingSessionDtoConverter.toDto(shoppingSession);
    }
}
