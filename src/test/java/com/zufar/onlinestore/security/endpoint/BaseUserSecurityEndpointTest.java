package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.OnlineStoreApplication;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.lessThan;

//todo: exception, when try to start from Docker
// Can't get Docker image: RemoteDockerImage(imageName=postgres:13.11-bullseye, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@39f8adc0)

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OnlineStoreApplication.class)
public class BaseUserSecurityEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;
    static RequestSpecification specification;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) throws InterruptedException {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    @BeforeEach
    void baseSetUp() {
        specification = given()
                .baseUri("http://localhost:" + port + UserSecurityEndpoint.USER_SECURITY_API_URL)
                .accept(ContentType.JSON);
    }

    public static ValidatableResponse checkStatusCodeInResponse(String url, int code, String schema, String mockRequest) {
        return given(specification)
                .contentType(ContentType.JSON)
                .body(mockRequest)
                .when()
                .post(url)
                .then()
                .statusCode(code)
                .body(matchesJsonSchemaInClasspath(schema))
                .time(lessThan(1500L));
    }

    public static ValidatableResponse checkStatusCodeInResponse(String url, int code, String mockRequest) {
        return given(specification)
                .contentType(ContentType.JSON)
                .body(mockRequest)
                .when()
                .post(url)
                .then()
                .statusCode(code)
                .time(lessThan(1500L));
    }
}
