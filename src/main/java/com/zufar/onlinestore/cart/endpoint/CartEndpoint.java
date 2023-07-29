package com.zufar.onlinestore.cart.endpoint;

import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = CartEndpoint.CART_URL)
public class CartEndpoint {

    public static final String CART_URL = "/api/v1/cart";

    private final CartApi cartApi;
    

    @PatchMapping
    @ResponseBody
    public ResponseEntity<ShoppingSessionDto> updateProductsQuantityInShoppingSessionItem(@RequestBody @Valid final UpdateProductsQuantityInShoppingSessionItemRequest request) {
        final UUID userId = request.userId();
        final UUID shoppingSessionItemId = request.shoppingSessionItemId();
        final Integer productsQuantityChange = request.productsQuantityChange();

        log.warn("Received the request to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession for the user with the id = {}.",
                productsQuantityChange, shoppingSessionItemId, userId);
        ShoppingSessionDto shoppingSessionDto = cartApi.updateProductsQuantityInShoppingSessionItem(userId, shoppingSessionItemId, productsQuantityChange);
        log.info("ProductsQuantity was updated in shoppingSession item");
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

}
