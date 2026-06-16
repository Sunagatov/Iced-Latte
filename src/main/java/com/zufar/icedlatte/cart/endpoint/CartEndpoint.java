package com.zufar.icedlatte.cart.endpoint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.cart.service.ShoppingCartService;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.AddNewItemsToShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.UpdateProductQuantityInShoppingCartItemRequest;
import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(CartEndpoint.CART_URL)
public class CartEndpoint implements com.zufar.icedlatte.openapi.cart.api.ShoppingCartApi {

    public static final String CART_URL = ApiPaths.CART;

    private final CurrentUserProvider currentUserProvider;
    private final ShoppingCartService shoppingCartService;

    @Override
    @PostMapping("/items")
    public ResponseEntity<ShoppingCartDto> addNewItemToShoppingCart(
            @Valid @RequestBody final AddNewItemsToShoppingCartRequest request) {
        var userId = currentUserProvider.getUserId();
        Set<AddCartItemRequest> cartItemRequests = toAddCartItemRequests(request);
        var shoppingCart = shoppingCartService.addItemsToCart(userId, cartItemRequests);
        log.debug("cart.items.added: cartId={}", shoppingCart.getId());
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @GetMapping
    public ResponseEntity<ShoppingCartDto> getShoppingCart() {
        var userId = currentUserProvider.getUserId();
        log.debug("cart.get: userId={}", userId);
        return ResponseEntity.ok(shoppingCartService.getByUserId(userId));
    }

    @Override
    @PatchMapping("/items")
    public ResponseEntity<ShoppingCartDto> updateProductQuantityInShoppingCartItem(
            @Valid @RequestBody final UpdateProductQuantityInShoppingCartItemRequest request) {
        var itemId = request.getShoppingCartItemId();
        var quantityChange = request.getProductQuantityChange();
        var userId = currentUserProvider.getUserId();
        log.debug("cart.items.quantity.updating: itemId={}, change={}", itemId, quantityChange);
        var shoppingCart = shoppingCartService.updateItemQuantity(itemId, userId, quantityChange);
        log.debug("cart.items.quantity.updated: itemId={}", itemId);
        return ResponseEntity.ok(shoppingCart);
    }

    @Override
    @DeleteMapping("/items")
    public ResponseEntity<ShoppingCartDto> deleteItemsFromShoppingCart(
            @Valid @RequestBody final DeleteItemsFromShoppingCartRequest request) {
        var userId = currentUserProvider.getUserId();
        List<UUID> shoppingCartItemIds = request.getShoppingCartItemIds();
        var shoppingCart = shoppingCartService.deleteItems(shoppingCartItemIds, userId);
        log.debug("cart.items.deleted");
        return ResponseEntity.ok(shoppingCart);
    }

    private static Set<AddCartItemRequest> toAddCartItemRequests(AddNewItemsToShoppingCartRequest request) {
        return request.getItems().stream()
                .collect(Collectors.toMap(
                        NewShoppingCartItemDto::getProductId, NewShoppingCartItemDto::getProductQuantity, Integer::sum))
                .entrySet()
                .stream()
                .map(CartEndpoint::toAddCartItemRequest)
                .collect(Collectors.toSet());
    }

    private static AddCartItemRequest toAddCartItemRequest(Map.Entry<UUID, Integer> item) {
        return new AddCartItemRequest(item.getKey(), item.getValue());
    }
}
