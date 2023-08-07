package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import com.zufar.onlinestore.reservation.entity.Reservation;
import com.zufar.onlinestore.reservation.repository.ReservationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.successfulReservation;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.ListUtils.removeAll;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.collections4.SetUtils.intersection;

@RequiredArgsConstructor
@Service
public class ReservationCreator {

    private static final String PRODUCT_ID = "product_id";
    private static final String RESERVED_QUANTITY = "reserved_quantity";

    private static final String PRODUCTS_RESERVING_SQL = """
            WITH updated_products AS (
            UPDATE product p SET old_quantity = p.quantity, quantity = p.quantity - LEAST(p.quantity, reservation.quantity)
            FROM (SELECT unnest(?) AS product_id, unnest(?) AS quantity) AS reservation
            WHERE p.id = reservation.product_id
            RETURNING p.id, (p.old_quantity - p.quantity) AS reserved_quantity
            )
            INSERT INTO reservation (reservation_id, product_id, reserved_quantity, created_at, status)
            SELECT ?, updated_products.id, updated_products.reserved_quantity, ?, cast('CREATED' as reservation_status)
            FROM updated_products
            WHERE updated_products.reserved_quantity > 0
            ON CONFLICT DO NOTHING
            RETURNING %s, %s;
            """.formatted(PRODUCT_ID, RESERVED_QUANTITY);

    private static final String UPDATE_RESERVED_PRODUCTS_SQL = """
            WITH updated_products AS (
            UPDATE product p SET old_quantity = p.quantity, quantity = p.quantity - LEAST(p.quantity, reservation.quantity)
            FROM (SELECT unnest(?) AS product_id, unnest(?) AS quantity) AS reservation
            WHERE p.id = reservation.product_id
            RETURNING p.id, (p.old_quantity - p.quantity) AS reserved_quantity
            )
            UPDATE reservation r
            SET reserved_quantity = r.reserved_quantity + updated_products.reserved_quantity, created_at = ?
            FROM updated_products
            WHERE reservation_id = ? AND r.product_id = updated_products.id
            RETURNING r.%s, r.%s;
            """.formatted(PRODUCT_ID, RESERVED_QUANTITY);

    private static final String PRODUCT_QUANTITY_RELEASING_SQL = """
            UPDATE product SET quantity = quantity + ?
            WHERE id = ?;
            """;

    private final ReservationRepository reservationRepository;
    private final ReservationTimeoutConfiguration timeoutConfiguration;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public CreatedReservationResponse tryToCreateReservation(final CreateReservationRequest request) {
        var reservationId = request.reservationId();
        var newReservations = request.productReservations();
        var reservationCreatedAt = Instant.now();
        var reservationExpiredAt = reservationCreatedAt.plus(timeoutConfiguration.defaultTimeout());

        var oldReservations = reservationRepository.findAllByReservationIdForUpdate(reservationId);

        var updatingReservations = getUpdatingReservations(newReservations, oldReservations);
        var insertingReservations = getInsertingReservations(newReservations, oldReservations);
        var deletingReservations = getDeletingReservations(newReservations, oldReservations);

        var updatedReservations = updateReservations(updatingReservations, oldReservations, reservationId, reservationCreatedAt);
        var insertedReservations = insertReservations(insertingReservations, reservationId, reservationCreatedAt);
        deleteReservations(deletingReservations);

        return successfulReservation(union(insertedReservations, updatedReservations), reservationExpiredAt);
    }

    private List<ProductReservation> getUpdatingReservations(final List<ProductReservation> newReservations, final List<Reservation> oldReservations) {
        var reservedProductIds = oldReservations.stream().map(Reservation::getProductId).collect(toSet());
        var reservingProductIds = newReservations.stream().map(ProductReservation::productId).collect(toSet());
        var updatingProductIds = intersection(reservedProductIds, reservingProductIds);
        return newReservations
                .stream()
                .filter(reservation -> updatingProductIds.contains(reservation.productId()))
                .toList();
    }

    private List<ProductReservation> getInsertingReservations(final List<ProductReservation> newReservations, final List<Reservation> oldReservations) {
        var reservedProductIds = oldReservations.stream().map(Reservation::getProductId).collect(toSet());
        var reservingProductIds = newReservations.stream().map(ProductReservation::productId).collect(toSet());
        var insertingProductIds = removeAll(reservingProductIds, reservedProductIds);
        return newReservations
                .stream()
                .filter(reservation -> insertingProductIds.contains(reservation.productId()))
                .toList();
    }

    private List<Reservation> getDeletingReservations(final List<ProductReservation> newReservations, final List<Reservation> oldReservations) {
        var reservedProductIds = oldReservations.stream().map(Reservation::getProductId).collect(toSet());
        var reservingProductIds = newReservations.stream().map(ProductReservation::productId).collect(toSet());
        var deletingProductIds = removeAll(reservedProductIds, reservingProductIds);
        return oldReservations
                .stream()
                .filter(reservation -> deletingProductIds.contains(reservation.getProductId()))
                .toList();
    }

    private List<ProductReservation> updateReservations(final List<ProductReservation> updatingReservations, List<Reservation> oldReservations, final UUID reservationId, final Instant reservationCreatedAt) {
        if (updatingReservations.isEmpty()) {
            return emptyList();
        }
        var productIdToReservedQuantity = oldReservations.stream()
                .collect(toMap(Reservation::getProductId, Reservation::getReservedQuantity));

        var productDiffReservations = updatingReservations.stream()
                .map(reservation -> {
                    var reservedQuantity = productIdToReservedQuantity.get(reservation.productId());
                    var reservationDiff = reservation.quantity() - reservedQuantity;
                    return new ProductReservation(reservation.productId(), reservationDiff);
                })
                .filter(reservation -> reservation.quantity() != 0)
                .toList();

        var updatedProductsResultSet = jdbcTemplate.queryForList(
                UPDATE_RESERVED_PRODUCTS_SQL,
                productDiffReservations.stream().map(ProductReservation::productId).toArray(UUID[]::new),
                productDiffReservations.stream().map(ProductReservation::quantity).toArray(Integer[]::new),
                Timestamp.from(reservationCreatedAt),
                reservationId
        );

        var updatedProducts = updatedProductsResultSet.stream()
                .map(updatedProductRow -> new ProductReservation(
                                (UUID) updatedProductRow.get(PRODUCT_ID),
                                (Integer) updatedProductRow.get(RESERVED_QUANTITY)
                        )
                ).toList();

        var updatingProductIds = updatingReservations.stream().map(ProductReservation::productId).collect(toSet());
        var updatedProductIds = updatedProducts.stream().map(ProductReservation::productId).collect(toSet());
        updatingProductIds.removeAll(updatedProductIds);
        var notUpdatedProducts = updatingProductIds.stream()
                .map(productId -> new ProductReservation(productId, productIdToReservedQuantity.get(productId)))
                .toList();

        return union(updatedProducts, notUpdatedProducts);
    }

    private List<ProductReservation> insertReservations(final List<ProductReservation> insertingReservations, final UUID reservationId, final Instant reservationCreatedAt) {
        if (insertingReservations.isEmpty()) {
            return emptyList();
        }
        var reservedProductsResultSet = jdbcTemplate.queryForList(
                PRODUCTS_RESERVING_SQL,
                insertingReservations.stream().map(ProductReservation::productId).toArray(UUID[]::new),
                insertingReservations.stream().map(ProductReservation::quantity).toArray(Integer[]::new),
                reservationId,
                Timestamp.from(reservationCreatedAt)
        );

        var reservedProducts = reservedProductsResultSet.stream()
                .map(reservedProductRow -> new ProductReservation(
                                (UUID) reservedProductRow.get(PRODUCT_ID),
                                (Integer) reservedProductRow.get(RESERVED_QUANTITY)
                        )
                ).toList();

        var reservingProductIds = insertingReservations.stream().map(ProductReservation::productId).collect(toSet());
        var reservedProductIds = reservedProducts.stream().map(ProductReservation::productId).collect(toSet());
        reservingProductIds.removeAll(reservedProductIds);
        var outOfStockProducts = reservingProductIds.stream().map(ProductReservation::outOfStockProductReservation).toList();

        return union(reservedProducts, outOfStockProducts);
    }

    private void deleteReservations(final List<Reservation> deletingReservations) {
        if (deletingReservations.isEmpty()) {
            return;
        }
        var productsReleasingBatch = deletingReservations.stream().map(this::buildProductReleasingBatchItem).toList();
        jdbcTemplate.batchUpdate(PRODUCT_QUANTITY_RELEASING_SQL, productsReleasingBatch);
        reservationRepository.deleteAllInBatch(deletingReservations);
        // TODO: optimize in single-query style like queries above
    }

    private Object[] buildProductReleasingBatchItem(final Reservation reservation) {
        return new Object[]{reservation.getReservedQuantity(), reservation.getProductId()};
    }
}

