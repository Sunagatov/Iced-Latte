package com.zufar.icedlatte.favorite.endpoint;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

import io.restassured.response.Response;

@DisplayName("Favorites endpoint integration tests")
class FavoritesEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String PRODUCT_ID_ONE = "418499f3-d951-40bf-9414-5cb90ab21ecb";
    private static final String PRODUCT_ID_TWO = "ad0ef2b7-816b-4a11-b361-dfcbe705fc96";

    @Test
    @DisplayName("Should return empty favorites for a fresh user")
    void shouldReturnEmptyFavoritesForFreshUser() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products", empty());
    }

    @Test
    @DisplayName("Should add favorites and deduplicate duplicate product ids in request")
    void shouldAddFavoritesAndDeduplicateDuplicateProductIdsInRequest() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        addFavorites(user, PRODUCT_ID_ONE, PRODUCT_ID_ONE, PRODUCT_ID_TWO);

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products", hasSize(2));

        List<String> productIds = getFavoriteProductIds(user);

        assertEquals(2, productIds.size());
        assertThat(productIds).containsExactlyInAnyOrder(PRODUCT_ID_ONE, PRODUCT_ID_TWO);
    }

    @Test
    @DisplayName("Should reject empty favorites request")
    void shouldRejectEmptyFavoritesRequest() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .body("""
                        {
                          "productIds": []
                        }
                        """)
                .post()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should reject oversized favorites request")
    void shouldRejectOversizedFavoritesRequest() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        String productIds = Stream.generate(() -> "\"" + PRODUCT_ID_ONE + "\"")
                .limit(101)
                .collect(Collectors.joining(", "));

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .body("""
                        {
                          "productIds": [%s]
                        }
                        """.formatted(productIds))
                .post()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should keep favorite lists isolated between users")
    void shouldKeepFavoriteListsIsolatedBetweenUsers() {
        AuthenticatedUser firstUser = registerAndAuthenticateUser();
        AuthenticatedUser secondUser = registerAndAuthenticateUser();

        addFavorites(firstUser, PRODUCT_ID_ONE);

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, secondUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products", empty());

        addFavorites(secondUser, PRODUCT_ID_TWO);

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, firstUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products", hasSize(1))
                .body("products[0].id", equalTo(PRODUCT_ID_ONE));

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, secondUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products", hasSize(1))
                .body("products[0].id", equalTo(PRODUCT_ID_TWO));
    }

    @Test
    @DisplayName("Should delete one favorite and keep the other intact")
    void shouldDeleteOneFavoriteAndKeepTheOtherIntact() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        addFavorites(user, PRODUCT_ID_ONE, PRODUCT_ID_TWO);

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .delete("/{productId}", PRODUCT_ID_ONE)
                .then()
                .statusCode(HttpStatus.OK.value());

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products", hasSize(1))
                .body("products[0].id", equalTo(PRODUCT_ID_TWO))
                .body("products[0].id", not(PRODUCT_ID_ONE));
    }

    @Test
    @DisplayName("Should keep one favorite item when duplicate add requests race")
    void shouldKeepOneFavoriteItemWhenDuplicateAddRequestsRace() throws Exception {
        AuthenticatedUser user = registerAndAuthenticateUser();
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Response> firstAdd = executor.submit(() -> addFavoriteWhenReleased(startGate, user));
            Future<Response> secondAdd = executor.submit(() -> addFavoriteWhenReleased(startGate, user));

            startGate.countDown();

            firstAdd.get(10, TimeUnit.SECONDS).then().statusCode(HttpStatus.OK.value());
            secondAdd.get(10, TimeUnit.SECONDS).then().statusCode(HttpStatus.OK.value());

            List<String> productIds = getFavoriteProductIds(user);

            assertEquals(1, productIds.size());
            assertThat(productIds).containsExactly(PRODUCT_ID_ONE);
        }
    }

    private void addFavorites(AuthenticatedUser user, String... productIds) {
        String body = """
                {
                  "productIds": [%s]
                }
        """.formatted(Arrays.stream(productIds)
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(", ")));

        given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .body(body)
                .post()
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    private List<String> getFavoriteProductIds(AuthenticatedUser user) {
        return given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("products.id", String.class);
    }

    private Response addFavoriteWhenReleased(CountDownLatch startGate, AuthenticatedUser user)
            throws InterruptedException {
        startGate.await();
        return given(authenticatedJsonSpec(FavoritesEndpoint.FAVORITES_URL, user.accessToken()))
                .body("""
                        {
                          "productIds": ["%s"]
                        }
                        """.formatted(PRODUCT_ID_ONE))
                .post();
    }
}
