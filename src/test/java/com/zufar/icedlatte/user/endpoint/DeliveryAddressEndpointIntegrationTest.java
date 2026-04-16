package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import io.restassured.common.mapper.TypeRef;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        createResponse.then()
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

        firstCreateResponse.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("isDefault", equalTo(true));

        secondCreateResponse.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("isDefault", equalTo(false));

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

        assertTrue(addresses.stream().anyMatch(address ->
                firstAddressId.equals(address.get("id")) && Boolean.FALSE.equals(address.get("isDefault"))));

        assertTrue(addresses.stream().anyMatch(address ->
                secondAddressId.equals(address.get("id")) && Boolean.TRUE.equals(address.get("isDefault"))));
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

        List<Map<String, Object>> remainingAddresses =
                given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                        .get()
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});

        assertTrue(remainingAddresses.stream().noneMatch(address -> addressId.equals(address.get("id"))));
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