package com.zufar.icedlatte.order.service.validator;

import java.time.LocalDate;

import com.zufar.icedlatte.common.exception.BadRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GetOrdersRequestValidator {

    public static void validate(
            final LocalDate dateFrom,
            final LocalDate dateTo) {

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException(
                    "'dateFrom' must be before or equal to 'dateTo'.");
        }
    }
}
