package com.zufar.onlinestore.cart.endpoint;

import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = CartEndpoint.CART_URL)
public class CartEndpoint {

    public static final String CART_URL = "/api/v1/cart";

    private final CartApi cartApi;

    @PostMapping(value = "/api/v1/cart/items/")
    public ResponseEntity<ShoppingSessionDto> addNewItemToShoppingSession(@RequestBody @Valid final AddNewItemToShoppingSessionRequest request) {
        log.warn("Received the request to add a new shoppingSessionItem with id: {} to the shoppingSession with the id = {}.",
                request.shoppingSessionId(), request.shoppingSessionId());

        ShoppingSessionDto shoppingSessionDto = cartApi.addNewItemToShoppingSession(request);
        log.info("ShoppingSessionItem with id={} was added to the shoppingSession with id={}",
                request.shoppingSessionItemId(), request.shoppingSessionId());
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

}
