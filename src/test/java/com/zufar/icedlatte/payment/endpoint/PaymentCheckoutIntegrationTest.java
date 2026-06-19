package com.zufar.icedlatte.payment.endpoint;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.stripe.exception.AuthenticationException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.cart.endpoint.CartEndpoint;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.checkout.StripeSessionGateway;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import com.zufar.icedlatte.user.api.UserLookupApi;

@DisplayName("Payment checkout integration tests")
class PaymentCheckoutIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";
    private static final String STRIPE_SESSION_ID = "cs_test_checkout_123";
    private static final String CHECKOUT_URL = "https://checkout.stripe.test/session/cs_test_checkout_123";
    private static final String RETRY_SESSION_ID = "cs_test_checkout_retry_456";
    private static final String RETRY_CHECKOUT_URL = "https://checkout.stripe.test/session/cs_test_checkout_retry_456";

    @MockitoBean
    private StripeSessionGateway stripeSessionGateway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserLookupApi userLookupApi;

    @DynamicPropertySource
    static void stripeProperties(DynamicPropertyRegistry registry) {
        registry.add("stripe.enabled", () -> "true");
    }

    @BeforeEach
    void setUpStripeGateway() throws StripeException {
        Session createdSession = new Session();
        createdSession.setId(STRIPE_SESSION_ID);
        createdSession.setUrl(CHECKOUT_URL);

        Session retrievedSession = new Session();
        retrievedSession.setId(STRIPE_SESSION_ID);
        retrievedSession.setUrl(CHECKOUT_URL);
        retrievedSession.setStatus("open");

        when(stripeSessionGateway.create(any(SessionCreateParams.class), anyString()))
                .thenReturn(createdSession);
        when(stripeSessionGateway.retrieve(STRIPE_SESSION_ID)).thenReturn(retrievedSession);
    }

    @Test
    @DisplayName("reuses persisted checkout for the same idempotency key")
    void reusesPersistedCheckoutForSameIdempotencyKey() throws StripeException {
        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        addCartItem(user, PRODUCT_ID, 2);

        var firstResponse = given(authenticatedJsonSpec("/api/v1/payment", user.accessToken()))
                .header("Idempotency-Key", "checkout-key-123")
                .body(checkoutRequestJson())
                .post("/checkout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();

        var secondResponse = given(authenticatedJsonSpec("/api/v1/payment", user.accessToken()))
                .header("Idempotency-Key", "checkout-key-123")
                .body(checkoutRequestJson())
                .post("/checkout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();

        UUID orderId = UUID.fromString(firstResponse.getString("orderId"));
        assertThat(secondResponse.getString("orderId")).isEqualTo(orderId.toString());
        assertThat(firstResponse.getString("stripeSessionId")).isEqualTo(STRIPE_SESSION_ID);
        assertThat(secondResponse.getString("stripeSessionId")).isEqualTo(STRIPE_SESSION_ID);
        assertThat(firstResponse.getString("checkoutUrl")).isEqualTo(CHECKOUT_URL);
        assertThat(secondResponse.getString("checkoutUrl")).isEqualTo(CHECKOUT_URL);

        Payment payment = paymentRepository
                .findByCheckoutIdempotencyKeyAndUserId("checkout-key-123", userId)
                .orElseThrow();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getProviderSessionId()).isEqualTo(STRIPE_SESSION_ID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.STRIPE_SESSION_CREATED);
        assertThat(payment.getCurrency()).isEqualTo("usd");
        assertThat(payment.getAmountMinor())
                .isEqualTo(BigDecimal.valueOf(15.98).movePointRight(2).longValueExact());

        assertThat(paymentRepository.findAll().stream()
                        .filter(saved -> userId.equals(saved.getUserId()))
                        .count())
                .isEqualTo(1);

        verify(stripeSessionGateway, times(1)).create(any(SessionCreateParams.class), anyString());
        verify(stripeSessionGateway, times(1)).retrieve(STRIPE_SESSION_ID);
    }

    @Test
    @DisplayName("retries a failed Stripe session with the same idempotency key without duplicating payment state")
    void retriesFailedStripeSessionWithoutDuplicatingPaymentState() throws StripeException {
        reset(stripeSessionGateway);

        Session createdSession = new Session();
        createdSession.setId(RETRY_SESSION_ID);
        createdSession.setUrl(RETRY_CHECKOUT_URL);

        when(stripeSessionGateway.create(any(SessionCreateParams.class), anyString()))
                .thenThrow(new AuthenticationException("boom", null, null, HttpStatus.UNAUTHORIZED.value()))
                .thenReturn(createdSession);

        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        addCartItem(user, PRODUCT_ID, 2);

        given(authenticatedJsonSpec("/api/v1/payment", user.accessToken()))
                .header("Idempotency-Key", "checkout-retry-key-123")
                .body(checkoutRequestJson())
                .post("/checkout")
                .then()
                .statusCode(HttpStatus.BAD_GATEWAY.value());

        Payment failedPayment = paymentRepository
                .findByCheckoutIdempotencyKeyAndUserId("checkout-retry-key-123", userId)
                .orElseThrow();
        UUID persistedOrderId = failedPayment.getOrderId();
        assertThat(failedPayment.getProviderSessionId()).isNull();
        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.CREATED);

        var retryResponse = given(authenticatedJsonSpec("/api/v1/payment", user.accessToken()))
                .header("Idempotency-Key", "checkout-retry-key-123")
                .body(checkoutRequestJson())
                .post("/checkout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();

        assertThat(retryResponse.getString("orderId")).isEqualTo(persistedOrderId.toString());
        assertThat(retryResponse.getString("stripeSessionId")).isEqualTo(RETRY_SESSION_ID);
        assertThat(retryResponse.getString("checkoutUrl")).isEqualTo(RETRY_CHECKOUT_URL);

        Payment recoveredPayment = paymentRepository
                .findByCheckoutIdempotencyKeyAndUserId("checkout-retry-key-123", userId)
                .orElseThrow();
        assertThat(recoveredPayment.getId()).isEqualTo(failedPayment.getId());
        assertThat(recoveredPayment.getOrderId()).isEqualTo(persistedOrderId);
        assertThat(recoveredPayment.getProviderSessionId()).isEqualTo(RETRY_SESSION_ID);
        assertThat(recoveredPayment.getStatus()).isEqualTo(PaymentStatus.STRIPE_SESSION_CREATED);
        assertThat(paymentRepository.findAll().stream()
                        .filter(saved -> userId.equals(saved.getUserId()))
                        .count())
                .isEqualTo(1);

        verify(stripeSessionGateway, times(2)).create(any(SessionCreateParams.class), anyString());
        verify(stripeSessionGateway, times(0)).retrieve(anyString());
    }

    private void addCartItem(AuthenticatedUser user, String productId, int quantity) {
        given(authenticatedJsonSpec(CartEndpoint.CART_URL, user.accessToken()))
                .body("""
                        {
                          "items": [
                            {
                              "productId": "%s",
                              "productQuantity": %d
                            }
                          ]
                        }
                        """.formatted(productId, quantity))
                .post("/items")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    private static String checkoutRequestJson() {
        return """
                {
                  "recipientName": "John",
                  "recipientSurname": "Doe",
                  "recipientPhone": "+44123456789",
                  "address": {
                    "country": "UK",
                    "city": "London",
                    "line": "221B Baker Street",
                    "postcode": "NW1 6XE"
                  }
                }
                """;
    }
}
