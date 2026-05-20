/**
 * TODO: Consider making this package internal in the future.
 * Currently exposed because cart (ShoppingCartItem) and favorite (FavoriteItemEntity)
 * have @ManyToOne ProductInfo — a JPA relationship that requires entity visibility.
 * To make internal:
 * 1. Replace @ManyToOne ProductInfo with @Column UUID productId in cart/favorite entities.
 * 2. Load product data (price, name) via ProductService when needed for DTOs/calculations.
 * Tradeoffs to consider:
 * - Performance: current EAGER join loads product in one query; ID reference adds a round-trip.
 * - Complexity: ItemsTotalPriceCalculator, ShoppingCartDtoConverter, FavoriteListDtoConverter
 *   all read product fields — they'd need ProductService injected or product data passed in.
 * - The DB FK constraint remains regardless — only the JPA mapping changes.
 * - For a monolith, @NamedInterface("entity") as shared kernel is a valid long-term choice.
 * Recommendation: only do this if extracting product into a separate service.
 */
@org.springframework.modulith.NamedInterface("entity")
package com.zufar.icedlatte.product.entity;
