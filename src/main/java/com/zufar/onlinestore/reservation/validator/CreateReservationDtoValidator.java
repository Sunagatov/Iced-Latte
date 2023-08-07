package com.zufar.onlinestore.reservation.validator;

import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationDto;
import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import org.springframework.stereotype.Component;

import static com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation.MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER;
import static com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation.MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER;
import static java.util.stream.Collectors.toSet;

@Component
public class CreateReservationDtoValidator implements IncomingDtoValidator<CreateReservationDto> {

    @Override
    public boolean isValid(final CreateReservationDto dto) {
        boolean containsInvalidQuantity = dto.productReservations().stream()
                .anyMatch(
                        product -> product.quantity() < MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER
                                || product.quantity() > MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER
                );

        if (containsInvalidQuantity) {
            return false;
        }

        return productIdMustNotBeDuplicated(dto);
    }

    private boolean productIdMustNotBeDuplicated(final CreateReservationDto dto) {
        int productIdsSize = dto
                .productReservations()
                .stream()
                .map(ProductReservation::productId)
                .collect(toSet())
                .size();
        return productIdsSize == dto.productReservations().size();
    }
}
