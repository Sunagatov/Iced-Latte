package com.zufar.icedlatte.user.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

@DisplayName("DeliveryAddressEndpoint integration tests")
class DeliveryAddressEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String BASE_PATH = "/api/v1/users/addresses";

    @Test
    @DisplayName("Should create first delivery address as default and return it in list")
    void shouldCreateFirstDeliveryAddressAsDefaultAndReturnItInList() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Response createResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Home", "221B Baker Street", "London", "NW1 6XE"))
                .post();

        createResponse
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("label", equalTo("Home"))
                .body("line", equalTo("221B Baker Street"))
                .body("city", equalTo("London"))
                .body("country", equalTo("United Kingdom"))
                .body("postcode", equalTo("NW1 6XE"))
                .body("isDefault", equalTo(true));

        String addressId = createResponse.jsonPath().getString("id");

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(1))
                .body("[0].id", equalTo(addressId))
                .body("[0].label", equalTo("Home"))
                .body("[0].isDefault", equalTo(true));
    }

    @Test
    @DisplayName("Should reject delivery address fields that contain only whitespace")
    void shouldRejectWhitespaceOnlyDeliveryAddressFields() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("   ", "221B Baker Street", "London", "NW1 6XE"))
                .post()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(0));
    }

    @Test
    @DisplayName("Should switch default address to another saved address")
    void shouldSwitchDefaultAddressToAnotherSavedAddress() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Response firstCreateResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Home", "10 Downing Street", "London", "SW1A 2AA"))
                .post();

        Response secondCreateResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Office", "1 Canada Square", "London", "E14 5AB"))
                .post();

        String firstAddressId = firstCreateResponse.jsonPath().getString("id");
        String secondAddressId = secondCreateResponse.jsonPath().getString("id");

        firstCreateResponse.then().statusCode(HttpStatus.CREATED.value()).body("isDefault", equalTo(true));

        secondCreateResponse.then().statusCode(HttpStatus.CREATED.value()).body("isDefault", equalTo(false));

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .patch("/{addressId}/default", secondAddressId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(secondAddressId))
                .body("label", equalTo("Office"))
                .body("isDefault", equalTo(true));

        List<Map<String, Object>> addresses = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {});

        long defaultCount = addresses.stream()
                .filter(address -> Boolean.TRUE.equals(address.get("isDefault")))
                .count();

        assertEquals(2, addresses.size());
        assertEquals(1, defaultCount);

        assertTrue(addresses.stream()
                .anyMatch(address ->
                        firstAddressId.equals(address.get("id")) && Boolean.FALSE.equals(address.get("isDefault"))));

        assertTrue(addresses.stream()
                .anyMatch(address ->
                        secondAddressId.equals(address.get("id")) && Boolean.TRUE.equals(address.get("isDefault"))));
    }

    @Test
    @DisplayName("Should keep default address when setting the same address as default again")
    void shouldKeepDefaultWhenSettingAlreadyDefaultAddressAgain() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Response createResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Home", "160 Piccadilly", "London", "W1J 9EB"))
                .post();

        String addressId = createResponse.jsonPath().getString("id");

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .patch("/{addressId}/default", addressId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(addressId))
                .body("isDefault", equalTo(true));

        List<Map<String, Object>> addresses = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {});

        long defaultCount = addresses.stream()
                .filter(address -> Boolean.TRUE.equals(address.get("isDefault")))
                .count();

        assertEquals(1, addresses.size());
        assertEquals(1, defaultCount);
        assertTrue(addresses.stream()
                .anyMatch(address ->
                        addressId.equals(address.get("id")) && Boolean.TRUE.equals(address.get("isDefault"))));
    }

    @Test
    @DisplayName("Should create exactly one default address when first addresses are created concurrently")
    void shouldCreateExactlyOneDefaultAddressForConcurrentFirstAddressRequests() throws Exception {
        AuthenticatedUser user = registerAndAuthenticateUser();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Response> homeFuture = executor.submit(() -> createAddressWhenReleased(
                    startGate, user.accessToken(), addressBody("Home", "1 Thread Lane", "London", "SE1 1AA")));
            Future<Response> officeFuture = executor.submit(() -> createAddressWhenReleased(
                    startGate, user.accessToken(), addressBody("Office", "2 Thread Lane", "London", "SE1 2AA")));

            startGate.countDown();

            Response homeResponse = homeFuture.get(10, TimeUnit.SECONDS);
            Response officeResponse = officeFuture.get(10, TimeUnit.SECONDS);

            homeResponse.then().statusCode(HttpStatus.CREATED.value());
            officeResponse.then().statusCode(HttpStatus.CREATED.value());

            List<Map<String, Object>> addresses = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                    .get()
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .body()
                    .as(new TypeRef<>() {});

            long defaultCount = addresses.stream()
                    .filter(address -> Boolean.TRUE.equals(address.get("isDefault")))
                    .count();

            assertEquals(2, addresses.size());
            assertEquals(1, defaultCount);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should update and then delete delivery address")
    void shouldUpdateAndThenDeleteDeliveryAddress() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Response createResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Home", "Old Street 1", "London", "EC1A 1AA"))
                .post();

        String addressId = createResponse.jsonPath().getString("id");

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Parents", "New Street 22", "Manchester", "M1 1AE"))
                .put("/{addressId}", addressId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(addressId))
                .body("label", equalTo("Parents"))
                .body("line", equalTo("New Street 22"))
                .body("city", equalTo("Manchester"))
                .body("country", equalTo("United Kingdom"))
                .body("postcode", equalTo("M1 1AE"));

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .delete("/{addressId}", addressId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        List<Map<String, Object>> remainingAddresses = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {});

        assertTrue(remainingAddresses.stream().noneMatch(address -> addressId.equals(address.get("id"))));
    }

    @Test
    @DisplayName("Should promote another address when default delivery address is deleted")
    void shouldPromoteAnotherAddressWhenDefaultDeliveryAddressIsDeleted() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Response firstCreateResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Home", "10 Downing Street", "London", "SW1A 2AA"))
                .post();

        Response secondCreateResponse = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(addressBody("Office", "1 Canada Square", "London", "E14 5AB"))
                .post();

        String firstAddressId = firstCreateResponse.jsonPath().getString("id");
        String secondAddressId = secondCreateResponse.jsonPath().getString("id");

        firstCreateResponse.then().statusCode(HttpStatus.CREATED.value()).body("isDefault", equalTo(true));
        secondCreateResponse.then().statusCode(HttpStatus.CREATED.value()).body("isDefault", equalTo(false));

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .delete("/{addressId}", firstAddressId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        List<Map<String, Object>> remainingAddresses = given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {});

        long defaultCount = remainingAddresses.stream()
                .filter(address -> Boolean.TRUE.equals(address.get("isDefault")))
                .count();

        assertEquals(1, remainingAddresses.size());
        assertEquals(1, defaultCount);
        assertTrue(remainingAddresses.stream()
                .anyMatch(address ->
                        secondAddressId.equals(address.get("id")) && Boolean.TRUE.equals(address.get("isDefault"))));
    }

    private Response createAddressWhenReleased(CountDownLatch startGate, String accessToken, String body)
            throws InterruptedException {
        startGate.await();
        return given(authenticatedJsonSpec(BASE_PATH, accessToken)).body(body).post();
    }

    private String addressBody(String label, String line, String city, String postcode) {
        return """
                {
                  "label": "%s",
                  "line": "%s",
                  "city": "%s",
                  "country": "%s",
                  "postcode": "%s"
                }
                """.formatted(label, line, city, "United Kingdom", postcode);
    }
}
