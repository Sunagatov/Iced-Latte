package com.zufar.onlinestore.reservation.validator;

import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import org.springframework.stereotype.Component;

import static com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation.MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER;
import static com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation.MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER;
import static java.util.stream.Collectors.toSet;

@Component
public class CreateReservationDtoValidator implements IncomingDtoValidator<CreateReservationRequest> {

    @Override
    public boolean isValid(final CreateReservationRequest dto) {
        boolean containsQuantityLessThanMin = dto.productReservations()
                .stream()
                .anyMatch(
                        product -> product.quantity() < MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER
                                || product.quantity() > MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER
                );

        if (containsQuantityLessThanMin) {
            return false;
        }
        return productIdMustNotBeDuplicated(dto);
    }

    private boolean productIdMustNotBeDuplicated(final CreateReservationRequest dto) {
        int productIdsSize = dto
                .productReservations()
                .stream()
                .map(ProductReservation::productId)
                .collect(toSet())
                .size();
        return productIdsSize == dto.productReservations().size();
    }
}
