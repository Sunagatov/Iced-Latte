package com.zufar.icedlatte.favorite.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.zufar.icedlatte.common.sync.SyncResource;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.product.api.ProductCatalogApi;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FavouriteSyncResource implements SyncResource {
    private final FavoriteService favourites;
    private final ProductCatalogApi products;

    @Override
    public String name() {
        return "favourites";
    }

    @Override
    public Object snapshot(UUID userId) {
        return favourites.getEnrichedFavoriteList(userId);
    }

    @Override
    public List<Conflict> validate(UUID userId, String type, JsonNode payload) {
        try {
            List<UUID> ids = ids(type, payload);
            Set<UUID> existing = favourites.getEnrichedFavoriteList(userId).getProducts().stream()
                    .map(product -> product.getId())
                    .collect(Collectors.toSet());
            if (type.equals("save") && !payload.path("saved").isBoolean()) throw new IllegalArgumentException();
            if (type.equals("save") && !payload.path("saved").booleanValue()) return List.of();
            Set<UUID> additions = new HashSet<>(ids);
            additions.removeAll(existing);
            List<Conflict> conflicts = new ArrayList<>();
            Set<UUID> found = products.findExistingProductIds(additions);
            for (UUID id : additions)
                if (!found.contains(id))
                    conflicts.add(new Conflict(
                            id.toString(),
                            "PRODUCT_UNAVAILABLE",
                            "This product is no longer available. Remove it from the guest draft."));
            if (existing.size() + additions.size() > 100)
                conflicts.add(
                        new Conflict(
                                null,
                                "FAVOURITES_LIMIT",
                                "Your account can hold 100 favourites. Choose fewer guest favourites or remove account favourites."));
            return conflicts;
        } catch (IllegalArgumentException ex) {
            return List.of(new Conflict(null, "INVALID_COMMAND", "Invalid favourites operation payload."));
        }
    }

    @Override
    public void apply(UUID userId, String type, JsonNode payload) {
        List<UUID> ids = ids(type, payload);
        if (type.equals("save") && !payload.path("saved").booleanValue()) favourites.delete(ids.getFirst(), userId);
        else if (!ids.isEmpty()) favourites.add(new ListOfFavoriteProducts().productIds(ids), userId);
    }

    private static List<UUID> ids(String type, JsonNode payload) {
        if (type.equals("save"))
            return List.of(UUID.fromString(payload.path("productId").asText()));
        JsonNode ids = payload.path("productIds");
        if (!type.equals("merge") || !ids.isArray() || ids.isEmpty() || ids.size() > 100)
            throw new IllegalArgumentException();
        List<UUID> result = new ArrayList<>();
        ids.forEach(id -> result.add(UUID.fromString(id.asText())));
        return result;
    }
}
