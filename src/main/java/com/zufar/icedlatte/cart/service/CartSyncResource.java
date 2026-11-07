package com.zufar.icedlatte.cart.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.common.sync.SyncResource;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartSyncResource implements SyncResource {
    private final ShoppingCartService carts;
    private final ProductCatalogApi products;

    @Override
    public String name() {
        return "cart";
    }

    @Override
    public Object snapshot(UUID userId) {
        return carts.getByUserId(userId);
    }

    @Override
    public List<Conflict> validate(UUID userId, String type, JsonNode payload) {
        try {
            ShoppingCartDto cart = carts.getByUserId(userId);
            Map<UUID, Integer> quantities = cart.getItems().stream()
                    .collect(
                            Collectors.toMap(item -> item.getProductInfo().getId(), item -> item.getProductQuantity()));
            if (type.equals("add") || type.equals("merge")) {
                Map<UUID, Integer> additions = additions(payload);
                Set<UUID> found = products.findExistingProductIds(additions.keySet());
                List<Conflict> conflicts = new ArrayList<>();
                additions.forEach((id, quantity) -> {
                    if (!found.contains(id))
                        conflicts.add(new Conflict(
                                id.toString(),
                                "PRODUCT_UNAVAILABLE",
                                "This product is no longer available. Remove it from the guest draft."));
                    else if (quantity < 1 || quantity > 99 || quantities.getOrDefault(id, 0) + quantity > 99)
                        conflicts.add(new Conflict(
                                id.toString(),
                                "QUANTITY_LIMIT",
                                "Combined quantity must be at most 99. Reduce this guest quantity."));
                });
                return conflicts;
            }
            if (type.equals("adjust")) {
                UUID id = uuid(payload.path("productId"));
                int delta = integer(payload.path("quantity"));
                if (!quantities.containsKey(id))
                    return List.of(new Conflict(
                            id.toString(), "PRODUCT_NOT_IN_CART", "This product is no longer in your cart."));
                int quantity = quantities.get(id) + delta;
                if (delta == 0 || delta < -99 || delta > 99 || quantity < 1 || quantity > 99)
                    return List.of(new Conflict(id.toString(), "QUANTITY_LIMIT", "Quantity must be between 1 and 99."));
            } else if (type.equals("clear")) ids(payload.path("productIds"));
            else if (type.equals("remove-slots")) ids(payload.path("slotIds"));
            else return List.of(new Conflict(null, "INVALID_COMMAND", "Unsupported cart operation."));
            return List.of();
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return List.of(new Conflict(null, "INVALID_COMMAND", "Invalid cart operation payload."));
        }
    }

    @Override
    public void apply(UUID userId, String type, JsonNode payload) {
        if (type.equals("add") || type.equals("merge")) {
            var items = additions(payload).entrySet().stream()
                    .map(item -> new AddCartItemRequest(item.getKey(), item.getValue()))
                    .collect(Collectors.toSet());
            carts.addItemsToCart(userId, items);
        } else if (type.equals("adjust")) {
            UUID id = uuid(payload.path("productId"));
            var item = carts.getByUserId(userId).getItems().stream()
                    .filter(row -> row.getProductInfo().getId().equals(id))
                    .findFirst()
                    .orElseThrow();
            carts.updateItemQuantity(item.getId(), userId, integer(payload.path("quantity")));
        } else {
            List<UUID> slots;
            if (type.equals("clear")) {
                Set<UUID> productsToRemove = new HashSet<>(ids(payload.path("productIds")));
                slots = carts.getByUserId(userId).getItems().stream()
                        .filter(item ->
                                productsToRemove.contains(item.getProductInfo().getId()))
                        .map(item -> item.getId())
                        .toList();
            } else slots = ids(payload.path("slotIds"));
            if (!slots.isEmpty()) carts.deleteItems(slots, userId);
        }
    }

    private static Map<UUID, Integer> additions(JsonNode payload) {
        JsonNode items = payload.path("items");
        if (!items.isArray() || items.isEmpty() || items.size() > 1000) throw new IllegalArgumentException();
        Map<UUID, Integer> result = new HashMap<>();
        items.forEach(item ->
                result.merge(uuid(item.path("productId")), integer(item.path("productQuantity")), Math::addExact));
        return result;
    }

    private static UUID uuid(JsonNode value) {
        return UUID.fromString(value.asText());
    }

    private static int integer(JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToInt()) throw new IllegalArgumentException();
        return value.intValue();
    }

    private static List<UUID> ids(JsonNode values) {
        if (!values.isArray() || values.size() > 1000) throw new IllegalArgumentException();
        List<UUID> result = new ArrayList<>();
        values.forEach(value -> result.add(uuid(value)));
        return result;
    }
}
