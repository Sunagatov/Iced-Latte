package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import com.zufar.onlinestore.reservation.entity.Reservation;
import com.zufar.onlinestore.reservation.repository.ReservationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.nothingReserved;
import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.reserved;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.ListUtils.removeAll;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.collections4.SetUtils.intersection;

@RequiredArgsConstructor
@Service
public class ReservationCreator {

    private static final String WAREHOUSE_ITEM_ID = "warehouse_item_id";
    private static final String RESERVED_QUANTITY = "reserved_quantity";

    private static final String PRODUCTS_RESERVING_SQL = """
            WITH updated_warehouse AS (
             UPDATE warehouse w SET tmp_quantity = w.quantity, quantity = w.quantity - LEAST(w.quantity, reservation.quantity)
             FROM (SELECT unnest(:warehouse_item_id) AS warehouse_item_id, unnest(:quantity) AS quantity) AS reservation
             WHERE w.id = reservation.warehouse_item_id
             RETURNING w.item_id, (w.tmp_quantity - w.quantity) AS reserved_quantity
            )
            INSERT INTO reservation (reservation_id, warehouse_item_id, reserved_quantity)
            SELECT :reservation_id, updated_warehouse.item_id, updated_warehouse.reserved_quantity)
            FROM updated_warehouse
            WHERE updated_warehouse.reserved_quantity > 0
            ON CONFLICT DO NOTHING
            RETURNING %s, %s;
            """.formatted(WAREHOUSE_ITEM_ID, RESERVED_QUANTITY);

    private static final String UPDATE_RESERVED_PRODUCTS_SQL = """
            WITH updated_warehouse AS (
             UPDATE warehouse w SET tmp_quantity = w.quantity, quantity = w.quantity - LEAST(w.quantity, reservation.quantity)
             FROM (SELECT unnest(:warehouse_item_id) AS warehouse_item_id, unnest(:quantity) AS quantity) AS reservation
             WHERE w.item_id = reservation.warehouse_item_id
             RETURNING w.item_id, (w.tmp_quantity - w.quantity) AS reserved_quantity
            )
            UPDATE reservation r
            SET reserved_quantity = r.reserved_quantity + updated_warehouse.reserved_quantity
            FROM updated_warehouse
            WHERE reservation_id = :reservation_id AND r.warehouse_item_id = updated_warehouse.item_id
            RETURNING r.%s, r.%s;
            """.formatted(WAREHOUSE_ITEM_ID, RESERVED_QUANTITY);

    private static final String PRODUCT_QUANTITY_RELEASING_SQL = """
            UPDATE warehouse SET quantity = quantity + :reserved_quantity
            WHERE item_id = :warehouse_item_id;
            """;

    private final UserReservationService userReservationService;
    private final ReservationRepository reservationRepository;
    private final ReservationTimeoutConfiguration timeoutConfiguration;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public CreatedReservationResponse createReservation(final CreateReservationRequest request) {
        var reservationInfo = userReservationService.getCurrentReservationForUpdate(request.userId());
        var reservationId = reservationInfo.reservationId();
        var newReservations = request.reservations();
        var reservationExpiredAt = reservationInfo.createdAt().plus(timeoutConfiguration.defaultTimeout());

        var oldReservations = reservationRepository.findAllByReservationId(reservationId);

        var updatingReservations = getUpdatingReservations(newReservations, oldReservations);
        var insertingReservations = getInsertingReservations(newReservations, oldReservations);
        var deletingReservations = getDeletingReservations(newReservations, oldReservations);

        var updatedReservations = updateReservations(updatingReservations, oldReservations, reservationId);
        var insertedReservations = insertReservations(insertingReservations, reservationId);
        deleteReservations(deletingReservations);

        var createdReservations = union(insertedReservations, updatedReservations);
        if (createdReservations.isEmpty()) {
            return nothingReserved();
        }
        return reserved(createdReservations, reservationExpiredAt);
    }

    private List<ProductReservation> getUpdatingReservations(final List<ProductReservation> newReservations, final List<Reservation> oldReservations) {
        var reservedProductIds = oldReservations.stream().map(Reservation::getWarehouseItemId).collect(toSet());
        var reservingProductIds = newReservations.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        var updatingProductIds = intersection(reservedProductIds, reservingProductIds);
        return newReservations
                .stream()
                .filter(reservation -> updatingProductIds.contains(reservation.warehouseItemId()))
                .toList();
    }

    private List<ProductReservation> getInsertingReservations(final List<ProductReservation> newReservations, final List<Reservation> oldReservations) {
        var reservedProductIds = oldReservations.stream().map(Reservation::getWarehouseItemId).collect(toSet());
        var reservingProductIds = newReservations.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        var insertingProductIds = removeAll(reservingProductIds, reservedProductIds);
        return newReservations
                .stream()
                .filter(reservation -> insertingProductIds.contains(reservation.warehouseItemId()))
                .toList();
    }

    private List<Reservation> getDeletingReservations(final List<ProductReservation> newReservations, final List<Reservation> oldReservations) {
        var reservedProductIds = oldReservations.stream().map(Reservation::getWarehouseItemId).collect(toSet());
        var reservingProductIds = newReservations.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        var deletingProductIds = removeAll(reservedProductIds, reservingProductIds);
        return oldReservations
                .stream()
                .filter(reservation -> deletingProductIds.contains(reservation.getWarehouseItemId()))
                .toList();
    }

    private List<ProductReservation> updateReservations(final List<ProductReservation> updatingReservations, List<Reservation> oldReservations, final UUID reservationId) {
        if (updatingReservations.isEmpty()) {
            return emptyList();
        }
        var productIdToReservedQuantity = oldReservations.stream()
                .collect(toMap(Reservation::getWarehouseItemId, Reservation::getReservedQuantity));

        var productDiffReservations = updatingReservations.stream()
                .map(reservation -> {
                    var reservedQuantity = productIdToReservedQuantity.get(reservation.warehouseItemId());
                    var reservationDiff = reservation.quantity() - reservedQuantity;
                    return new ProductReservation(reservation.warehouseItemId(), reservationDiff);
                })
                .filter(reservation -> reservation.quantity() != 0)
                .toList();

        var sqlParams = Map.of(
                "warehouse_item_id", productDiffReservations.stream().map(ProductReservation::warehouseItemId).toArray(UUID[]::new),
                "quantity", productDiffReservations.stream().map(ProductReservation::quantity).toArray(Integer[]::new),
                "reservation_id", reservationId
        );

        var updatedProductsResultSet = jdbcTemplate.queryForList(UPDATE_RESERVED_PRODUCTS_SQL, sqlParams);

        var updatedProducts = updatedProductsResultSet.stream()
                .map(updatedProductRow -> new ProductReservation(
                                (UUID) updatedProductRow.get(WAREHOUSE_ITEM_ID),
                                (Integer) updatedProductRow.get(RESERVED_QUANTITY)
                        )
                ).toList();

        var updatingProductIds = updatingReservations.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        var updatedProductIds = updatedProducts.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        updatingProductIds.removeAll(updatedProductIds);
        var notUpdatedProducts = updatingProductIds.stream()
                .map(productId -> new ProductReservation(productId, productIdToReservedQuantity.get(productId)))
                .toList();

        return union(updatedProducts, notUpdatedProducts);
    }

    private List<ProductReservation> insertReservations(final List<ProductReservation> insertingReservations, final UUID reservationId) {
        if (insertingReservations.isEmpty()) {
            return emptyList();
        }
        var sqlParams = Map.of(
                "warehouse_item_id", insertingReservations.stream().map(ProductReservation::warehouseItemId).toArray(UUID[]::new),
                "quantity", insertingReservations.stream().map(ProductReservation::quantity).toArray(Integer[]::new),
                "reservation_id", reservationId
        );

        var reservedProductsResultSet = jdbcTemplate.queryForList(PRODUCTS_RESERVING_SQL, sqlParams);

        var reservedProducts = reservedProductsResultSet.stream()
                .map(reservedProductRow -> new ProductReservation(
                                (UUID) reservedProductRow.get(WAREHOUSE_ITEM_ID),
                                (Integer) reservedProductRow.get(RESERVED_QUANTITY)
                        )
                ).toList();

        var reservingProductIds = insertingReservations.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        var reservedProductIds = reservedProducts.stream().map(ProductReservation::warehouseItemId).collect(toSet());
        reservingProductIds.removeAll(reservedProductIds);
        var outOfStockProducts = reservingProductIds.stream().map(ProductReservation::outOfStockProductReservation).toList();

        return union(reservedProducts, outOfStockProducts);
    }

    private void deleteReservations(final List<Reservation> deletingReservations) {
        if (deletingReservations.isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, ?>[] productsReleasingBatch = deletingReservations.stream().map(this::buildProductReleasingBatchItem).toArray(Map[]::new);
        jdbcTemplate.batchUpdate(PRODUCT_QUANTITY_RELEASING_SQL, productsReleasingBatch);
        reservationRepository.deleteAllInBatch(deletingReservations);
    }

    private Map<String, ?> buildProductReleasingBatchItem(final Reservation reservation) {
        return Map.of("reserved_quantity", reservation.getReservedQuantity(),
                "warehouse_item_id", reservation.getWarehouseItemId());
    }
}

