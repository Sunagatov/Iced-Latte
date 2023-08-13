package com.zufar.onlinestore.cart.endpoint;

import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.AddNewItemsToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import com.zufar.onlinestore.user.entity.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = CartEndpoint.CART_URL)
public class CartEndpoint {

    public static final String CART_URL = "/api/v1/cart";

    private final CartApi cartApi;

    @PostMapping(value = "/items")
    public ResponseEntity<ShoppingSessionDto> addNewItemToShoppingSession(@RequestBody @Valid final AddNewItemsToShoppingSessionRequest request) {
        log.warn("Received the request to add a new items to the shoppingSession");
        Set<NewShoppingSessionItemDto> items = request.items();
        ShoppingSessionDto shoppingSessionDto = cartApi.addItemsToShoppingSession(items);
        log.info("ShoppingSessionItem was added to the shoppingSession with id={}", shoppingSessionDto.id());
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

    @GetMapping
    public ResponseEntity<ShoppingSessionDto> getShoppingSession(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = ((UserEntity) userDetails).getUserId();
        log.info("Received the request to get the shoppingSession for the user with id: {}", userId);
        ShoppingSessionDto shoppingSessionDto = cartApi.getShoppingSessionByUserId(userId);
        log.info("The shoppingSession for the user with id: {} was retrieved successfully", shoppingSessionDto.userId());
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

    @PatchMapping(value = "/items")
    public ResponseEntity<ShoppingSessionDto> updateProductsQuantityInShoppingSessionItem(@RequestBody @Valid final UpdateProductsQuantityInShoppingSessionItemRequest request) {
        UUID shoppingSessionItemId = request.shoppingSessionItemId();
        Integer productsQuantityChange = request.productsQuantityChange();
        log.warn("Received the request to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {}.",
                productsQuantityChange, shoppingSessionItemId);
        ShoppingSessionDto shoppingSessionDto = cartApi.updateProductsQuantityInShoppingSessionItem(shoppingSessionItemId, productsQuantityChange);
        log.info("ProductsQuantity was updated in shoppingSession item");
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }

    @DeleteMapping(value = "/items")
    public ResponseEntity<ShoppingSessionDto> deleteItemsFromShoppingSession(@RequestBody @Valid final DeleteItemsFromShoppingSessionRequest request) {
        log.info("Received the request to delete the shopping session items with ids: {}.", request.shoppingSessionItemIds());
        ShoppingSessionDto shoppingSessionDto = cartApi.deleteItemsFromShoppingSession(request);
        log.info("The shopping session items with ids = {} were deleted.", request.shoppingSessionItemIds());
        return ResponseEntity.ok()
                .body(shoppingSessionDto);
    }
}
