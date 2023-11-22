package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.openapi.dto.AddNewItemsToShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.UpdateProductQuantityInShoppingCartItemRequest;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
public class CartEndpoint implements com.zufar.icedlatte.openapi.cart.api.ShoppingCartApi {

    public static final String CART_URL = "/api/v1/cart";

    private final com.zufar.icedlatte.cart.api.CartApi cartApi;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    @PostMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> addNewItemToShoppingCart(@RequestBody final AddNewItemsToShoppingCartRequest request) {
        log.warn("Received the request to add a new items to the shoppingCart");
        Set<NewShoppingCartItemDto> items = request.getItems();
        ShoppingCartDto shoppingCartDto = cartApi.addItemsToShoppingCart(items);
        log.info("ShoppingCartItem was added to the shoppingCart with id={}", shoppingCartDto.getId());
        return ResponseEntity.ok()
                .body(shoppingCartDto);
    }

    @Override
    @GetMapping
    public ResponseEntity<ShoppingCartDto> getShoppingCart() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to get the shoppingCart for the user with id: {}", userId);
        ShoppingCartDto shoppingCartDto = cartApi.getShoppingCartByUserId(userId);
        log.info("The shoppingCart for the user with id: {} was retrieved successfully", shoppingCartDto.getUserId());
        return ResponseEntity.ok()
                .body(shoppingCartDto);
    }

    @Override
    @PatchMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> updateProductQuantityInShoppingCartItem(@RequestBody final UpdateProductQuantityInShoppingCartItemRequest request) {
        UUID shoppingCartItemId = request.getShoppingCartItemId();
        Integer productQuantityChange = request.getProductQuantityChange();
        log.warn("Received the request to update the productQuantity with the change = {} in the shoppingCartItem with id: {}.",
                productQuantityChange, shoppingCartItemId);
        ShoppingCartDto shoppingCartDto = cartApi.updateProductQuantityInShoppingCartItem(shoppingCartItemId, productQuantityChange);
        log.info("ProductsQuantity was updated in shoppingCart item");
        return ResponseEntity.ok()
                .body(shoppingCartDto);
    }

    @Override
    @DeleteMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> deleteItemsFromShoppingCart(@RequestBody final DeleteItemsFromShoppingCartRequest request) {
        log.info("Received the request to delete the shopping cart items with ids: {}.", request.getShoppingCartItemIds());
        ShoppingCartDto shoppingCartDto = cartApi.deleteItemsFromShoppingCart(request);
        log.info("The shopping cart items with ids = {} were deleted.", request.getShoppingCartItemIds());
        return ResponseEntity.ok()
                .body(shoppingCartDto);
    }
}
