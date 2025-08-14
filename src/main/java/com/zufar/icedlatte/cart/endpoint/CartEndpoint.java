package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.cart.api.*;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.validation.Valid;
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

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final AddItemsToShoppingCartHelper addItemsToShoppingCartHelper;
    private final ProductQuantityItemUpdater productQuantityItemUpdater;
    private final ShoppingCartProvider shoppingCartProvider;
    private final ShoppingCartItemsDeleter shoppingCartItemsDeleter;

    @Override
    @PostMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> addNewItemToShoppingCart(@Valid @RequestBody final AddNewItemsToShoppingCartRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.warn("Invalid add request: empty or null items");
            return ResponseEntity.badRequest().build();
        }
        log.info("Adding {} items to shopping cart", request.getItems().size());
        var shoppingCart = addItemsToShoppingCartHelper.add(request.getItems());
        log.info("Items added to shopping cart: {}", shoppingCart.getId());
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @GetMapping
    public ResponseEntity<ShoppingCartDto> getShoppingCart() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("Getting shopping cart for user: {}", userId);
        var shoppingCart = shoppingCartProvider.getByUserId(userId);
        log.info("Shopping cart retrieved for user: {}", userId);
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @PatchMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> updateProductQuantityInShoppingCartItem(@Validated @Valid @RequestBody final UpdateProductQuantityInShoppingCartItemRequest request) {
        var itemId = request.getShoppingCartItemId();
        var quantityChange = request.getProductQuantityChange();
        log.info("Updating item quantity: {} by {}", itemId, quantityChange);
        var shoppingCart = productQuantityItemUpdater.update(itemId, quantityChange);
        log.info("Item quantity updated for item: {}", itemId);
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @DeleteMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> deleteItemsFromShoppingCart(@Valid @RequestBody final DeleteItemsFromShoppingCartRequest request) {
        // Validate input to prevent code injection
        if (request.getShoppingCartItemIds() == null || request.getShoppingCartItemIds().isEmpty()) {
            log.warn("Invalid delete request: empty or null item IDs");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Deleting {} items from shopping cart", request.getShoppingCartItemIds().size());
        var shoppingCart = shoppingCartItemsDeleter.delete(request);
        log.info("Items deleted from shopping cart");
        return ResponseEntity.ok(shoppingCart);
    }
}
