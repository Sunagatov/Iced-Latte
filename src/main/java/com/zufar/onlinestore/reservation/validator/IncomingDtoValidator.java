package com.zufar.onlinestore.reservation.validator;

public interface IncomingDtoValidator<DTO> {

    boolean isValid(DTO dto);
}
