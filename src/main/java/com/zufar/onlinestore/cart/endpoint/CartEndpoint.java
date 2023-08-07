package com.zufar.onlinestore.cart.endpoint;

import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
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

    @GetMapping
    public ResponseEntity<ShoppingSessionDto> getShoppingSession(@RequestParam final String userId) {
        log.info("Received the request to get the shoppingSession for the user with id: {}", userId);
        ShoppingSessionDto shoppingSessionDto = cartApi.getShoppingSessionByUserId(UUID.fromString(userId));
        log.info("The shoppingSession for the user with id: {} was retrieved successfully", shoppingSessionDto.userId());
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

    @PatchMapping
    public ResponseEntity<ShoppingSessionDto> updateProductsQuantityInShoppingSessionItem(@RequestBody @Valid final UpdateProductsQuantityInShoppingSessionItemRequest request) {
        log.warn("Received the request to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                request.productsQuantityChange(), request.shoppingSessionItemId(), request.shoppingSessionId());

        ShoppingSessionDto shoppingSessionDto = cartApi.updateProductsQuantityInShoppingSessionItem(request);
        log.info("ProductsQuantity was updated in shoppingSession item");
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

    @DeleteMapping
    public ResponseEntity<ShoppingSessionDto> deleteItemInShoppingSession (@RequestBody @Valid final RemoveItemFromShoppingSessionRequest request) {
        log.warn("Received the request to delete the shoppingSessionItem with id: {}.",
                request.shoppingSessionItemId());
        ShoppingSessionDto shoppingSessionDto = cartApi.removeItemFromShoppingSession(request);
        log.info("The shoppingSessionItem with id = {} was deleted.",
                request.shoppingSessionItemId());
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

}
