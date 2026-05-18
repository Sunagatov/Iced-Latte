package com.zufar.icedlatte.payment.endpoint;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.payment.api.CheckoutPaymentService;
import com.zufar.icedlatte.payment.api.PaymentStatusService;
import com.zufar.icedlatte.payment.api.StripeWebhookService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

@DisplayName("PaymentEndpoint validation contract tests")
class PaymentEndpointValidationContractTest {

    @Test
    @DisplayName("checkout endpoint validation metadata matches generated OpenAPI interface")
    void createCheckoutValidationMetadataMatchesGeneratedApi() throws NoSuchMethodException {
        PaymentEndpoint endpoint = new PaymentEndpoint(
                mock(CheckoutPaymentService.class),
                mock(PaymentStatusService.class),
                mock(StripeWebhookService.class)
        );
        var method = PaymentEndpoint.class.getMethod(
                "createCheckout",
                String.class,
                CreateCheckoutRequestDto.class
        );
        var request = new CreateCheckoutRequestDto("Zufar", "Sunagatov")
                .recipientPhone("07405503609")
                .address(new AddressDto()
                        .line("704, Cassia Point, 2 Glasshouse Gardens")
                        .city("London")
                        .postcode("E20 1HU")
                        .country("United Kingdom"));

        assertThatCode(() -> {
            try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
                validatorFactory.getValidator().forExecutables()
                        .validateParameters(endpoint, method, new Object[]{"checkout-test-key", request});
            }
        }).doesNotThrowAnyException();
    }
}
