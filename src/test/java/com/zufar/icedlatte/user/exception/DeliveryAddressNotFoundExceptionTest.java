package com.zufar.icedlatte.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeliveryAddressNotFoundException")
class DeliveryAddressNotFoundExceptionTest {

    @Test
    @DisplayName("renders missing address id in message")
    void rendersMissingAddressIdInMessage() {
        UUID addressId = UUID.randomUUID();

        assertThat(new DeliveryAddressNotFoundException(addressId))
                .hasMessage("Delivery address with id = " + addressId + " is not found.");
    }
}
