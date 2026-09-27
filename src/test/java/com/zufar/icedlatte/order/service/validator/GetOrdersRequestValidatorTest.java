package com.zufar.icedlatte.order.service.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;

class GetOrdersRequestValidatorTest {

    @Test
    void dateFromAfterDateToThrowsBadRequestException() {
        LocalDate dateFrom = LocalDate.of(2026, 9, 20);
        LocalDate dateTo = LocalDate.of(2026, 9, 10);

        assertThatThrownBy(() -> GetOrdersRequestValidator.validate(dateFrom, dateTo))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void dateFromBeforeDateToDoesNotThrow() {
        assertThatCode(() ->
                GetOrdersRequestValidator.validate(
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void equalDatesDoNotThrow() {
        LocalDate date = LocalDate.of(2026, 9, 10);

        assertThatCode(() ->
                GetOrdersRequestValidator.validate(date, date))
                .doesNotThrowAnyException();
    }

    @Test
    void nullDateFromDoesNotThrow() {
        assertThatCode(() ->
                GetOrdersRequestValidator.validate(
                        null,
                        LocalDate.of(2026, 9, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void nullDateToDoesNotThrow() {
        assertThatCode(() ->
                GetOrdersRequestValidator.validate(
                        LocalDate.of(2026, 9, 10),
                        null))
                .doesNotThrowAnyException();
    }
}
