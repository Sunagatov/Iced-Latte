package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.common.http.ApiPaths;
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
@RequestMapping(CartEndpoint.CART_URL)
public class CartEndpoint implements com.zufar.icedlatte.openapi.cart.api.ShoppingCartApi {

    public static final String CART_URL = ApiPaths.CART;

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ShoppingCartService shoppingCartService;

    @Override
    @PostMapping("/items")
    public ResponseEntity<ShoppingCartDto> addNewItemToShoppingCart(@Valid @RequestBody final AddNewItemsToShoppingCartRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        var shoppingCart = shoppingCartService.addItems(userId, request.getItems());
        log.debug("cart.items.added: cartId={}", shoppingCart.getId());
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @GetMapping
    public ResponseEntity<ShoppingCartDto> getShoppingCart() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("cart.get: userId={}", userId);
        return ResponseEntity.ok(shoppingCartService.getByUserId(userId));
    }

    @Override
    @PatchMapping("/items")
    public ResponseEntity<ShoppingCartDto> updateProductQuantityInShoppingCartItem(@Valid @RequestBody final UpdateProductQuantityInShoppingCartItemRequest request) {
        var itemId = request.getShoppingCartItemId();
        var quantityChange = request.getProductQuantityChange();
        var userId = securityPrincipalProvider.getUserId();
        log.debug("cart.items.quantity.updating: itemId={}, change={}", itemId, quantityChange);
        var shoppingCart = shoppingCartService.updateItemQuantity(itemId, userId, quantityChange);
        log.debug("cart.items.quantity.updated: itemId={}", itemId);
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @DeleteMapping("/items")
    public ResponseEntity<ShoppingCartDto> deleteItemsFromShoppingCart(@Valid @RequestBody final DeleteItemsFromShoppingCartRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        var shoppingCart = shoppingCartService.deleteItems(request, userId);
        log.debug("cart.items.deleted");
        return ResponseEntity.ok(shoppingCart);
    }
}
