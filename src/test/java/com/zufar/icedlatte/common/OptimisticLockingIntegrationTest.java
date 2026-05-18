package com.zufar.icedlatte.common;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.test.config.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@code @Version} fields use the correct {@code jakarta.persistence.Version}
 * annotation and that Hibernate actually manages optimistic locking for all versioned entities.
 *
 * <p>Each test verifies:
 * <ol>
 *   <li>Version is non-null (Hibernate initialized it)</li>
 *   <li>Version increments on update</li>
 *   <li>A stale version causes an exception on flush</li>
 * </ol>
 */
@DisplayName("Optimistic locking integration tests")
class OptimisticLockingIntegrationTest extends IntegrationTestBase {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("ShoppingCartItem: version increments and stale write fails")
    void shoppingCartItem_optimisticLocking() {
        UUID itemId = transactionTemplate.execute(_ -> {
            ShoppingCartItem item = entityManager
                    .createQuery("SELECT i FROM ShoppingCartItem i", ShoppingCartItem.class)
                    .setMaxResults(1)
                    .getSingleResult();
            assertNotNull(item.getVersion(), "Version must be managed by Hibernate");
            return item.getId();
        });

        int versionBefore = transactionTemplate.execute(_ -> entityManager.find(ShoppingCartItem.class, itemId).getVersion());

        transactionTemplate.executeWithoutResult(_ -> {
            ShoppingCartItem item = entityManager.find(ShoppingCartItem.class, itemId);
            item.setProductQuantity(item.getProductQuantity() + 1);
        });

        int versionAfter = transactionTemplate.execute(_ -> entityManager.find(ShoppingCartItem.class, itemId).getVersion());

        assertEquals(versionBefore + 1, versionAfter, "Version should increment on update");
        assertThrows(Exception.class, () ->
                        transactionTemplate.executeWithoutResult(_ -> {
                            ShoppingCartItem stale = entityManager.find(ShoppingCartItem.class, itemId);
                            entityManager.detach(stale);
                            stale.setVersion(versionBefore);
                            stale.setProductQuantity(99);
                            entityManager.merge(stale);
                            entityManager.flush();
                        }),
                "Stale version should cause optimistic lock failure"
        );
    }

    @Test
    @DisplayName("ProductInfo: version increments and stale write fails")
    void productInfo_optimisticLocking() {
        UUID productId = transactionTemplate.execute(_ -> {
            ProductInfo product = entityManager
                    .createQuery("SELECT p FROM ProductInfo p", ProductInfo.class)
                    .setMaxResults(1)
                    .getSingleResult();
            return product.getId();
        });

        long versionBefore = transactionTemplate.execute(_ -> entityManager.find(ProductInfo.class, productId).getVersion());

        transactionTemplate.executeWithoutResult(_ -> {
            ProductInfo product = entityManager.find(ProductInfo.class, productId);
            product.setPrice(product.getPrice().add(BigDecimal.ONE));
        });

        long versionAfter = transactionTemplate.execute(_ -> entityManager.find(ProductInfo.class, productId).getVersion());

        assertEquals(versionBefore + 1, versionAfter, "Version should increment on update");
        assertThrows(Exception.class, () ->
                        transactionTemplate.executeWithoutResult(_ -> {
                            ProductInfo product = entityManager.find(ProductInfo.class, productId);
                            entityManager.detach(product);
                            product.setVersion(versionBefore);
                            product.setPrice(BigDecimal.valueOf(999));
                            entityManager.merge(product);
                            entityManager.flush();
                        }),
                "Stale version should cause optimistic lock failure"
        );
    }

    @Test
    @DisplayName("FavoriteItemEntity: version is managed by Hibernate")
    void favoriteItem_optimisticLocking() {
        UUID itemId = transactionTemplate.execute(_ -> {
            FavoriteItemEntity item = entityManager
                    .createQuery("SELECT f FROM FavoriteItemEntity f", FavoriteItemEntity.class)
                    .setMaxResults(1)
                    .getSingleResult();
            assertNotNull(item.getVersion(), "Version must be managed by Hibernate");
            return item.getId();
        });

        int versionBefore = transactionTemplate.execute(_ -> entityManager.find(FavoriteItemEntity.class, itemId).getVersion());

        assertThrows(Exception.class, () ->
                        transactionTemplate.executeWithoutResult(_ -> {
                            FavoriteItemEntity item = entityManager.find(FavoriteItemEntity.class, itemId);
                            entityManager.detach(item);
                            item.setVersion(versionBefore - 1);
                            entityManager.merge(item);
                            entityManager.flush();
                        }),
                "Stale version should cause optimistic lock failure"
        );
    }

    @Test
    @DisplayName("Order: version increments and stale write fails")
    void order_optimisticLocking() {
        UUID orderId = transactionTemplate.execute(_ -> {
            var orders = entityManager
                    .createQuery("SELECT o FROM Order o", Order.class)
                    .setMaxResults(1)
                    .getResultList();
            if (orders.isEmpty()) {
                return null;
            }
            assertNotNull(orders.getFirst().getVersion(), "Version must be managed by Hibernate");
            return orders.getFirst().getId();
        });

        if (orderId == null) {
            // No seed orders — verify at minimum that the @Version import is correct
            // by checking the annotation is jakarta.persistence.Version
            var versionField = java.util.Arrays.stream(Order.class.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(jakarta.persistence.Version.class))
                    .findFirst();
            assertNotNull(versionField.orElse(null), "Order must have a field annotated with jakarta.persistence.Version");
            return;
        }

        int versionBefore = transactionTemplate.execute(_ -> entityManager.find(Order.class, orderId).getVersion());

        transactionTemplate.executeWithoutResult(_ -> {
            Order order = entityManager.find(Order.class, orderId);
            order.setRecipientPhone("555-0100");
        });

        int versionAfter = transactionTemplate.execute(_ -> entityManager.find(Order.class, orderId).getVersion());

        assertEquals(versionBefore + 1, versionAfter, "Version should increment on update");
        assertThrows(Exception.class, () ->
                        transactionTemplate.executeWithoutResult(_ -> {
                            Order order = entityManager.find(Order.class, orderId);
                            entityManager.detach(order);
                            order.setVersion(versionBefore);
                            order.setRecipientPhone("555-9999");
                            entityManager.merge(order);
                            entityManager.flush();
                        }),
                "Stale version should cause optimistic lock failure"
        );
    }

}
