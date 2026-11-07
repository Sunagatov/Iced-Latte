package com.zufar.icedlatte.cart.service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.cart.exception.InvalidCartItemRequestException;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartMergeService {

    private final JdbcTemplate jdbcTemplate;
    private final ShoppingCartService shoppingCartService;

    @Transactional
    public ShoppingCartDto merge(UUID userId, UUID key, Set<AddCartItemRequest> items) {
        String requestItems = items.stream()
                .map(item -> item.productId() + ":" + item.productQuantity())
                .sorted()
                .collect(Collectors.joining(","));

        // The unique key serializes concurrent retries. Receipt and cart commit together.
        int inserted = jdbcTemplate.update("""
                INSERT INTO cart_merge_receipt (user_id, merge_key, request_items)
                VALUES (?, ?, ?) ON CONFLICT (user_id, merge_key) DO NOTHING
                """, userId, key, requestItems);
        if (inserted == 1) {
            return shoppingCartService.addItemsToCart(userId, items);
        }

        String original = jdbcTemplate.queryForObject("""
                SELECT request_items FROM cart_merge_receipt WHERE user_id = ? AND merge_key = ?
                """, String.class, userId, key);
        if (!requestItems.equals(original)) {
            throw new InvalidCartItemRequestException("Idempotency-Key cannot be reused with different cart items.");
        }
        // Return today's cart, rather than replaying a stale snapshot after later edits.
        return shoppingCartService.getByUserId(userId);
    }
}
