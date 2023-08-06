package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import com.zufar.onlinestore.reservation.entity.Reservation;
import com.zufar.onlinestore.reservation.entity.ReservationStatus;
import com.zufar.onlinestore.reservation.exception.ReservationAbortedException;
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
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static org.apache.commons.collections4.ListUtils.removeAll;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.collections4.SetUtils.intersection;

@RequiredArgsConstructor
@Service
public class ReservationCreator {

    private static final String PRODUCT_QUANTITY_RESERVING_SQL = """
            UPDATE product SET quantity = quantity - ?
            WHERE (quantity - ?) >= 0 AND id = ?;
            """;

    private static final String PRODUCT_QUANTITY_RELEASING_SQL = """
            UPDATE product SET quantity = quantity + ?
            WHERE id = ?;
            """;

    private static final String SAVE_RESERVATION_SQL = """ 
            INSERT INTO reservation (reservation_id, product_id, reserved_quantity, created_at, status)
            VALUES (?, ?, ?, ?, cast(? AS reservation_status)) ON CONFLICT DO NOTHING;
            """;

    private static final String UPDATE_RESERVATION_SQL = """ 
            UPDATE reservation SET reserved_quantity = reserved_quantity + ?
            WHERE reservation_id = ? AND product_id = ?;
            """;

    /**
     * Sign that exactly one row is updated
     */
    private static final int EXACTLY_ONE_ROW = 1;
    private static final boolean NOT_UPDATED_ROWS = false;
    private static final int PRODUCT_ID_POSITION_IN_PRODUCT_BATCH = 2;


    private final ProductInfoRepository productInfoRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationTimeoutConfiguration timeoutConfiguration;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = ReservationAbortedException.class)
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
        releaseUnnecessaryReservations(deletingReservations);

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

        var productIdToReservingQuantity = productDiffReservations.stream()
                .collect(toMap(ProductReservation::productId, ProductReservation::quantity));

        var productsReservingBatch = productDiffReservations.stream().map(this::buildProductReservingBatchItem).toList();

        var updatedCounts = jdbcTemplate.batchUpdate(PRODUCT_QUANTITY_RESERVING_SQL, productsReservingBatch);

        var updatedAndNotUpdatedRows = range(0, updatedCounts.length).boxed()
                .collect(partitioningBy(this::isUpdatedRow));

        var notUpdatedProductIds = updatedAndNotUpdatedRows.get(NOT_UPDATED_ROWS).stream()
                .map(rowIndex -> getProductId(productsReservingBatch, rowIndex))
                .toList();

        var notUpdatedProducts = productInfoRepository.findAllByIdForUpdate(notUpdatedProductIds);

        notUpdatedProducts.forEach(product -> {
            var quantity = product.getQuantity();
            if (quantity != 0) {
                product.setQuantity(quantity); // auto update managed entity inside transaction
                productIdToReservingQuantity.replace(product.getId(), quantity);
            } else productIdToReservingQuantity.replace(product.getId(), 0);
        });

        var reservationsBatch = productIdToReservingQuantity.entrySet().stream()
                .filter(reservation -> reservation.getValue() != 0)
                .map(reservation -> buildUpdateReservationBatchItem(reservation.getValue(), reservationId, reservation.getKey()))
                .toList();

        jdbcTemplate.batchUpdate(UPDATE_RESERVATION_SQL, reservationsBatch);

        return productIdToReservingQuantity.entrySet().stream()
                .map(reservation -> new ProductReservation(
                        reservation.getKey(),
                        productIdToReservedQuantity.get(reservation.getKey()) + reservation.getValue()))
                .toList();
    }

    private List<ProductReservation> insertReservations(final List<ProductReservation> insertingReservations, final UUID reservationId, final Instant reservationCreatedAt) {
        var productsReservingBatch = insertingReservations.stream().map(this::buildProductReservingBatchItem).toList();
        var productIdToReservingQuantity = insertingReservations.stream()
                .collect(toMap(ProductReservation::productId, ProductReservation::quantity));

        var updatedCounts = jdbcTemplate.batchUpdate(PRODUCT_QUANTITY_RESERVING_SQL, productsReservingBatch);

        var updatedAndNotUpdatedRows = range(0, updatedCounts.length).boxed()
                .collect(partitioningBy(this::isUpdatedRow));

        var notUpdatedProductIds = updatedAndNotUpdatedRows.get(NOT_UPDATED_ROWS).stream()
                .map(rowIndex -> getProductId(productsReservingBatch, rowIndex))
                .toList();

        var notUpdatedProducts = productInfoRepository.findAllByIdForUpdate(notUpdatedProductIds);

        notUpdatedProducts.forEach(product -> {
            var quantity = product.getQuantity();
            if (quantity != 0) {
                product.setQuantity(0); // auto update managed entity inside transaction
                productIdToReservingQuantity.replace(product.getId(), quantity);
            } else productIdToReservingQuantity.replace(product.getId(), 0);
        });

        var reservationsBatch = productIdToReservingQuantity.entrySet().stream()
                .filter(reservation -> reservation.getValue() != 0)
                .map(reservation -> buildSaveReservationBatchItem(reservationId, reservation.getKey(), reservation.getValue(), reservationCreatedAt))
                .toList();

        var reservationUpdatedCounts = jdbcTemplate.batchUpdate(SAVE_RESERVATION_SQL, reservationsBatch);

        var hasConflictOnInsertion = stream(reservationUpdatedCounts)
                .anyMatch(updatedRowCount -> !isUpdatedRow(updatedRowCount));

        if (hasConflictOnInsertion) {
            throw new ReservationAbortedException(reservationId);
        }

        return productIdToReservingQuantity.entrySet().stream()
                .map(reservation -> new ProductReservation(reservation.getKey(), reservation.getValue()))
                .toList();
    }

    private void releaseUnnecessaryReservations(final List<Reservation> deletingReservations) {
        var productsReleasingBatch = deletingReservations.stream().map(this::buildProductReleasingBatchItem).toList();
        jdbcTemplate.batchUpdate(PRODUCT_QUANTITY_RELEASING_SQL, productsReleasingBatch);
        reservationRepository.deleteAllInBatch(deletingReservations);
    }

    private boolean isUpdatedRow(final int updatedRowCount) {
        return updatedRowCount == EXACTLY_ONE_ROW;
    }

    /**
     * Matches in order of {@link ReservationCreator#PRODUCT_QUANTITY_RESERVING_SQL} wildcards (?)
     * Position of productId = 2
     */
    private Object[] buildProductReservingBatchItem(final ProductReservation reservation) {
        return new Object[]{reservation.quantity(), reservation.quantity(), reservation.productId()};
    }

    /**
     * Matches in order of {@link ReservationCreator#PRODUCT_QUANTITY_RELEASING_SQL} wildcards (?)
     */
    private Object[] buildProductReleasingBatchItem(final Reservation reservation) {
        return new Object[]{reservation.getReservedQuantity(), reservation.getProductId()};
    }

    /**
     * Matches in order of {@link ReservationCreator#SAVE_RESERVATION_SQL} wildcards (?)
     */
    private Object[] buildSaveReservationBatchItem(final UUID reservationId, final UUID productId, final Integer quantity, final Instant reservationCreatedAt) {
        return new Object[]{reservationId, productId, quantity, Timestamp.from(reservationCreatedAt), ReservationStatus.CREATED.name()};
    }

    /**
     * Matches in order of {@link ReservationCreator#UPDATE_RESERVATION_SQL} wildcards (?)
     */
    private Object[] buildUpdateReservationBatchItem(final Integer quantity, final UUID reservationId, final UUID productId) {
        return new Object[]{quantity, reservationId, productId};
    }


    private UUID getProductId(List<Object[]> productsBatch, Integer rowIndex) {
        return (UUID) productsBatch.get(rowIndex)[PRODUCT_ID_POSITION_IN_PRODUCT_BATCH];
    }
}

