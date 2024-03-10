package com.zufar.icedlatte.ai.reviewvalidator.api;

import com.zufar.icedlatte.ai.reviewvalidator.exception.AiServiceConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
public class AiServiceConnectionProvider {

    @Value("${ai.url}")
    protected String aiServiceUrl;

    @Value("${ai.api_key}")
    protected String aiServiceApiKey;

    public HttpURLConnection getConnection() {
        HttpURLConnection httpURLConnection;
        try {

            httpURLConnection = (HttpURLConnection) new URL(aiServiceUrl).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + aiServiceApiKey);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setDoOutput(true);

        } catch (Exception exception) {
            log.error("Failed to open httpURLConnection with url = '{}'", aiServiceUrl, exception);
            throw new AiServiceConnectionException(aiServiceUrl);
        }
        return httpURLConnection;
    }
}
