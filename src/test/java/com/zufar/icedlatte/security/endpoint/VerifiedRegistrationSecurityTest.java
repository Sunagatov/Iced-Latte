package com.zufar.icedlatte.security.endpoint;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

@TestPropertySource(properties = "email.enabled=true")
class VerifiedRegistrationSecurityTest extends AuthenticatedUserIntegrationSupport {
    @Test
    void existingAndNewAddressesHaveIdenticalPublicRegistrationResponses() {
        var existing = registerAndAuthenticateUser();
        var first = given(jsonSpec("/api/v1/auth"))
                .body(new UserRegistrationRequest("Test", "User", existing.email(), "gentle river coffee morning"))
                .post("/register");
        var second = given(jsonSpec("/api/v1/auth"))
                .body(new UserRegistrationRequest(
                        "Test", "User", "new-" + UUID.randomUUID() + "@example.com", "gentle river coffee morning"))
                .post("/register");
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(first.asString()).isEmpty();
        assertThat(second.asString()).isEmpty();
        assertThat(first.cookies()).isEmpty();
        assertThat(second.cookies()).isEmpty();
    }
}
