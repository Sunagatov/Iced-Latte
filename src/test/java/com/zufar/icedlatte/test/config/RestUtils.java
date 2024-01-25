package com.zufar.icedlatte.test.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.zufar.icedlatte.security.endpoint.UserSecurityEndpoint;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static io.restassured.RestAssured.given;

@ActiveProfiles("test")
@SpringBootTest
public class RestUtils {

    private static final String AUTHENTICATE_TEMPLATE = "/security/model/authenticate-template.json";

    public static String getJwtToken(Integer port, String email, String password) {
        var specification = given()
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

        String jwtToken = response.getBody().path("token");

        if (isJwtTokenNotValid(jwtToken)) {
            throw new IllegalArgumentException("JWT Token is empty or null. Test failed.");
        }

        return jwtToken;
    }

    private static boolean isJwtTokenNotValid(String jwtToken) {
        return jwtToken == null || jwtToken.isEmpty();
    }

    public static String getRequestBody(String resourcePath) {
        try {
            JsonNode json = JsonLoader.fromResource(resourcePath);
            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
