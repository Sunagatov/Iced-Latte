package com.zufar.icedlatte.sync;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

import io.restassured.response.Response;

class SyncIntegrationTest extends AuthenticatedUserIntegrationSupport {
    private static final String A = "418499f3-d951-40bf-9414-5cb90ab21ecb";
    private static final String B = "ad0ef2b7-816b-4a11-b361-dfcbe705fc96";

    @Autowired
    JdbcTemplate jdbc;

    private String owner(AuthenticatedUser user) {
        return given(authenticatedJsonSpec("/api/v1/users", user.accessToken()))
                .get()
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    private long revision(AuthenticatedUser user, String resource) {
        return given(authenticatedJsonSpec("/api/v1/sync", user.accessToken()))
                .get("/" + resource)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("revision");
    }

    private Map<String, Object> command(String owner, long revision, String type, Map<String, Object> payload) {
        return new HashMap<>(Map.of(
                "operationId",
                UUID.randomUUID().toString(),
                "expectedUserId",
                owner,
                "expectedRevision",
                revision,
                "type",
                type,
                "payload",
                payload));
    }

    private Response send(AuthenticatedUser user, String resource, Map<String, Object> command) {
        return given(authenticatedJsonSpec("/api/v1/sync", user.accessToken()))
                .body(command)
                .post("/" + resource + "/operations");
    }

    private Map<String, Object> items(String id, int quantity) {
        return Map.of("items", List.of(Map.of("productId", id, "productQuantity", quantity)));
    }

    @Test
    void lostAdditionResponseCanBeRetriedAndClearUsesProductsRatherThanUnknownSlotIds() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        var add = command(owner, revision(user, "cart"), "add", items(A, 2));
        send(user, "cart", add).then().statusCode(200).body("outcome", equalTo("APPLIED"));
        // Pretend the response above was lost after the real transaction committed.
        var replay = send(user, "cart", add)
                .then()
                .statusCode(200)
                .body("snapshot.data.productsQuantity", equalTo(2))
                .extract()
                .jsonPath();
        var clear = command(owner, replay.getLong("snapshot.revision"), "clear", Map.of("productIds", List.of(A)));
        send(user, "cart", clear).then().statusCode(200).body("snapshot.data.productsQuantity", equalTo(0));
        send(user, "cart", add)
                .then()
                .statusCode(200)
                .body("outcome", equalTo("APPLIED"))
                .body("snapshot.data.productsQuantity", equalTo(0));
        given(authenticatedJsonSpec("/api/v1/sync", user.accessToken()))
                .get("/cart/operations/" + add.get("operationId"))
                .then()
                .statusCode(200)
                .body("outcome", equalTo("APPLIED"))
                .body("snapshot.data.productsQuantity", equalTo(0));
        var another = registerAndAuthenticateUser();
        given(authenticatedJsonSpec("/api/v1/sync", another.accessToken()))
                .get("/cart/operations/" + add.get("operationId"))
                .then()
                .statusCode(404);
    }

    @Test
    void concurrentRetriesApplyOnceAndIndependentStaleCommandsAreRejected() throws Exception {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        long initial = revision(user, "cart");
        var add = command(owner, initial, "add", items(A, 1));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> send(user, "cart", add));
            var retry = pool.submit(() -> send(user, "cart", add));
            first.get(15, TimeUnit.SECONDS).then().statusCode(200).body("outcome", equalTo("APPLIED"));
            retry.get(15, TimeUnit.SECONDS).then().statusCode(200).body("snapshot.data.productsQuantity", equalTo(1));
        }
        send(user, "cart", command(owner, initial, "add", items(B, 1)))
                .then()
                .statusCode(200)
                .body("outcome", equalTo("REJECTED"))
                .body("code", equalTo("REVISION_CONFLICT"));
    }

    @Test
    void rejectsWrongOwnerAndChangedPayloadOrBaseRevision() {
        var user = registerAndAuthenticateUser();
        var add = command(owner(user), revision(user, "cart"), "add", items(A, 1));
        var wrongOwner = new HashMap<>(add);
        wrongOwner.put("expectedUserId", UUID.randomUUID().toString());
        send(user, "cart", wrongOwner).then().statusCode(409);
        send(user, "cart", add).then().statusCode(200);
        var changed = new HashMap<>(add);
        changed.put("payload", items(A, 2));
        send(user, "cart", changed).then().statusCode(409);
        changed.put("payload", add.get("payload"));
        changed.put("expectedRevision", 1);
        send(user, "cart", changed).then().statusCode(409);
    }

    @Test
    void rejectedGuestMergeHasAStoredOutcomeAndCorrectedDraftUsesANewOperation() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        var unavailable = command(
                owner, revision(user, "cart"), "merge", items(UUID.randomUUID().toString(), 1));
        send(user, "cart", unavailable)
                .then()
                .statusCode(200)
                .body("outcome", equalTo("REJECTED"))
                .body("code", equalTo("PRODUCT_UNAVAILABLE"));
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM sync_operation WHERE user_id = ? AND outcome = 'REJECTED'",
                        Long.class,
                        UUID.fromString(owner)))
                .isEqualTo(1L);
        send(user, "cart", unavailable).then().statusCode(200).body("outcome", equalTo("REJECTED"));
        send(user, "cart", command(owner, revision(user, "cart"), "merge", items(A, 1)))
                .then()
                .statusCode(200)
                .body("outcome", equalTo("APPLIED"));
    }

    @Test
    void legacyCartWritersAdvanceTheSameRevision() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        long before = revision(user, "cart");
        given(authenticatedJsonSpec("/api/v1/cart", user.accessToken()))
                .body(items(A, 1))
                .post("/items")
                .then()
                .statusCode(200);
        assertThat(revision(user, "cart")).isGreaterThan(before);
        send(user, "cart", command(owner, before, "add", items(B, 1)))
                .then()
                .statusCode(200)
                .body("outcome", equalTo("REJECTED"));
    }

    @Test
    void favouriteSetAndRemovalAreReplaySafe() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        var save = command(owner, revision(user, "favourites"), "save", Map.of("productId", A, "saved", true));
        send(user, "favourites", save).then().statusCode(200).body("outcome", equalTo("APPLIED"));
        var remove = command(owner, revision(user, "favourites"), "save", Map.of("productId", A, "saved", false));
        send(user, "favourites", remove).then().statusCode(200).body("snapshot.data.products.size()", equalTo(0));
        send(user, "favourites", save).then().statusCode(200).body("snapshot.data.products.size()", equalTo(0));
    }

    @Test
    void profileUpdatesUseRevisionsAndRejectAnOlderEditor() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        long before = revision(user, "profile");
        var update = command(
                owner,
                before,
                "update",
                Map.of(
                        "firstName",
                        "Updated",
                        "lastName",
                        "Person",
                        "birthDate",
                        "1990-01-15",
                        "phoneNumber",
                        "+442071234567",
                        "address",
                        Map.of("country", "UK", "city", "London", "line", "1 Test Street", "postcode", "SW1A 1AA")));
        send(user, "profile", update)
                .then()
                .statusCode(200)
                .body("outcome", equalTo("APPLIED"))
                .body("snapshot.data.firstName", equalTo("Updated"))
                .body("snapshot.data.birthDate", equalTo("1990-01-15"))
                .body("snapshot.data.address.city", equalTo("London"));
        send(user, "profile", command(owner, before, "update", Map.of("firstName", "Stale", "lastName", "Person")))
                .then()
                .statusCode(200)
                .body("code", equalTo("REVISION_CONFLICT"));
        given(authenticatedJsonSpec("/api/v1/users", user.accessToken()))
                .get()
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Updated"))
                .body("profileRevision", equalTo(1));
    }

    @Test
    void receiptFailureRollsBackTheBusinessWriteAndRetryCanApplyOnce() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        var add = command(owner, revision(user, "cart"), "add", items(A, 3));
        jdbc.execute("""
                CREATE FUNCTION sync_test_receipt_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'simulated receipt write failure'; END $$
                """);
        jdbc.execute("""
                CREATE TRIGGER sync_test_receipt_failure BEFORE INSERT ON sync_operation
                FOR EACH ROW EXECUTE FUNCTION sync_test_receipt_failure()
                """);
        try {
            send(user, "cart", add).then().statusCode(500);
            assertThat(revision(user, "cart")).isZero();
            assertThat(jdbc.queryForObject(
                            "SELECT count(*) FROM sync_operation WHERE user_id = ?",
                            Long.class,
                            UUID.fromString(owner)))
                    .isZero();
            given(authenticatedJsonSpec("/api/v1/sync", user.accessToken()))
                    .get("/cart")
                    .then()
                    .statusCode(200)
                    .body("data.productsQuantity", equalTo(0));
        } finally {
            jdbc.execute("DROP TRIGGER sync_test_receipt_failure ON sync_operation");
            jdbc.execute("DROP FUNCTION sync_test_receipt_failure()");
        }
        send(user, "cart", add).then().statusCode(200).body("snapshot.data.productsQuantity", equalTo(3));
        send(user, "cart", add).then().statusCode(200).body("snapshot.data.productsQuantity", equalTo(3));
    }

    @Test
    void quantityRejectionLeavesTheAccountCartEditableForGuestRecovery() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        send(user, "cart", command(owner, revision(user, "cart"), "add", items(A, 99)))
                .then()
                .statusCode(200);
        var merge = command(owner, revision(user, "cart"), "merge", items(A, 2));
        send(user, "cart", merge).then().statusCode(200).body("code", equalTo("QUANTITY_LIMIT"));
        send(user, "cart", command(owner, revision(user, "cart"), "adjust", Map.of("productId", A, "quantity", -2)))
                .then()
                .statusCode(200)
                .body("snapshot.data.productsQuantity", equalTo(97));
        send(user, "cart", merge).then().statusCode(200).body("outcome", equalTo("REJECTED"));
        send(user, "cart", command(owner, revision(user, "cart"), "merge", items(A, 2)))
                .then()
                .statusCode(200)
                .body("snapshot.data.productsQuantity", equalTo(99));
    }

    @Test
    void invalidProfileIsAStoredRejectionAndSnapshotIsBoundToItsAuthenticatedOwner() {
        var user = registerAndAuthenticateUser();
        String owner = owner(user);
        var invalid =
                command(owner, revision(user, "profile"), "update", Map.of("firstName", "", "lastName", "Person"));
        send(user, "profile", invalid)
                .then()
                .statusCode(200)
                .body("code", equalTo("INVALID_PROFILE"))
                .body("snapshot.userId", equalTo(owner));
        send(user, "profile", invalid).then().statusCode(200).body("outcome", equalTo("REJECTED"));
        jsonSpec("/api/v1/sync").get("/cart").then().statusCode(401);
    }
}
