package com.zufar.icedlatte.ai.api;

import com.zufar.icedlatte.ai.dto.AiDto;
import com.zufar.icedlatte.ai.dto.AiResult;
import com.zufar.icedlatte.ai.dto.TextModerationResult;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChecker implements AiApi {

    @Value("${ai.url}")
    protected String url;
    @Value("${ai.api_key}")
    protected String apiKey;
    @Value("${ai.request_body}")
    protected String requestBody;
    @Value("${ai.message_replacement_pattern}")
    protected String messageReplacementPattern;

    @Override
    public AiDto checkMessage(String message) {
        try {
            var request = requestBody.replace(messageReplacementPattern, message);
            HttpURLConnection con = getHttpURLConnection(url, apiKey, request);
            int responseCode = con.getResponseCode();

            log.debug("Response Code: " + responseCode);

      try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }

        log.debug("Response: " + response);

        Gson gson = new Gson();
        TextModerationResult result =
            gson.fromJson(response.toString(), TextModerationResult.class);

        AiResult air = AiResult.OK;
        if (result.getResults().stream().anyMatch(TextModerationResult.Result::isFlagged)) {
          air = AiResult.NOT_APPROPRIATE;
        }
        return new AiDto(air, response.toString());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new AiDto(AiResult.ERROR, e.getMessage());
        }
    }

    private static HttpURLConnection getHttpURLConnection(String url, String apiKey,
                                                          String requestBody) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Bearer " + apiKey);
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(requestBody);
            wr.flush();
        }
        return con;
    }
}
