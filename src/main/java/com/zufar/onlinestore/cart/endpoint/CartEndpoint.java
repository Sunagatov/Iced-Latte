package com.zufar.onlinestore.cart.endpoint;

import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.AddNewProductToShoppingSessionRequest;
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

    @PostMapping(value = "/api/v1/cart/items/")
    public ResponseEntity<ShoppingSessionDto> addNewItemToShoppingSession(@RequestBody @Valid final AddNewProductToShoppingSessionRequest request) {
        final UUID userId = request.userId();
        final UUID productId = UUID.fromString(request.productId());

        log.warn("Received the request to add a new product with id: {} to the shoppingSession for the user with the id = {}.",
                productId, userId);

        ShoppingSessionDto shoppingSessionDto = cartApi.addNewProductToShoppingSession(userId, productId);
        log.info("ShoppingSessionItem was added to the shoppingSession with id={}", shoppingSessionDto.id());
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
