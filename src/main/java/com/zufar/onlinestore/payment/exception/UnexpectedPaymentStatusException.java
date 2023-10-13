package com.zufar.onlinestore.payment.exception;

import com.zufar.onlinestore.openapi.dto.ProcessedPaymentDetailsDto;
import lombok.Getter;

@Getter
public class UnexpectedPaymentStatusException extends RuntimeException {

    private final ProcessedPaymentDetailsDto.StatusEnum paymentStatus;

    public UnexpectedPaymentStatusException(final ProcessedPaymentDetailsDto.StatusEnum paymentStatus) {
        super(String.format("Payment status = %s is unexpected.", paymentStatus));
        this.paymentStatus = paymentStatus;
    }
}
