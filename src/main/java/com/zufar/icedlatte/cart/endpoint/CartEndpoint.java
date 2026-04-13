package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.cart.api.AddItemsToShoppingCartHelper;
import com.zufar.icedlatte.cart.api.ProductQuantityItemUpdater;
import com.zufar.icedlatte.cart.api.ShoppingCartItemsDeleter;
import com.zufar.icedlatte.cart.api.ShoppingCartProvider;
import com.zufar.icedlatte.openapi.dto.AddNewItemsToShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.UpdateProductQuantityInShoppingCartItemRequest;
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
            log.warn("cart.items.add.invalid: reason=empty_items");
            return ResponseEntity.badRequest().build();
        }
        log.debug("cart.items.adding: count={}", request.getItems().size());
        var userId = securityPrincipalProvider.getUserId();
        var shoppingCart = addItemsToShoppingCartHelper.add(userId, request.getItems());
        log.debug("cart.items.added: cartId={}", shoppingCart.getId());
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @GetMapping
    public ResponseEntity<ShoppingCartDto> getShoppingCart() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("cart.get: userId={}", userId);
        return ResponseEntity.ok(shoppingCartProvider.getByUserId(userId));
    }

    @Override
    @PatchMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> updateProductQuantityInShoppingCartItem(@Validated @Valid @RequestBody final UpdateProductQuantityInShoppingCartItemRequest request) {
        var itemId = request.getShoppingCartItemId();
        var quantityChange = request.getProductQuantityChange();
        var userId = securityPrincipalProvider.getUserId();
        log.debug("cart.items.quantity.updating: itemId={}, change={}", itemId, quantityChange);
        var shoppingCart = productQuantityItemUpdater.update(itemId, userId, quantityChange);
        log.debug("cart.items.quantity.updated: itemId={}", itemId);
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @DeleteMapping(value = "/items")
    public ResponseEntity<ShoppingCartDto> deleteItemsFromShoppingCart(@Valid @RequestBody final DeleteItemsFromShoppingCartRequest request) {
        if (request.getShoppingCartItemIds().isEmpty()) {
            log.warn("cart.items.delete.invalid: reason=empty_ids");
            return ResponseEntity.badRequest().build();
        }
        var userId = securityPrincipalProvider.getUserId();
        log.debug("cart.items.deleting: count={}", request.getShoppingCartItemIds().size());
        var shoppingCart = shoppingCartItemsDeleter.delete(request, userId);
        log.debug("cart.items.deleted");
        return ResponseEntity.ok(shoppingCart);
    }
}
