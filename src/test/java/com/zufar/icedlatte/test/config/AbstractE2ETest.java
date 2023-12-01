package com.zufar.icedlatte.test.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.zufar.icedlatte.security.endpoint.UserSecurityEndpoint;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static io.restassured.RestAssured.given;


@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.secret}")
    protected String secretKey;

    @Value("${jwt.expiration}")
    protected Long expiration;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;
    protected static String jwtToken = "";

    private static final String AUTHENTICATE_TEMPLATE = "/security/model/authenticate-template.json";

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    protected static RequestSpecification specification;

    protected String getRequestBody(String resourcePath) {
        try {
            JsonNode json = JsonLoader.fromResource(resourcePath);
            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void getJwtToken(){
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(UserSecurityEndpoint.USER_SECURITY_API_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        String body = getRequestBody(AUTHENTICATE_TEMPLATE)
                .formatted(email, password);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        jwtToken = response.getBody().path("token");
    }
}
