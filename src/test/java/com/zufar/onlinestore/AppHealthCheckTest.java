package com.zufar.onlinestore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class AppHealthCheckTest {

    @Test
    public void shouldReturnHttpOkForHealthCheck() throws Exception {
        URI uri = new URI("http://localhost:8083/api/v1/products/1e5b295f-8f50-4425-90e9-8b590a27b3a9");
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        assertEquals(HttpURLConnection.HTTP_OK, responseCode, "Health check failed");
    }
}
