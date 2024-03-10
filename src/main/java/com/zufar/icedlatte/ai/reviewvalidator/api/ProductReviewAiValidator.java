package com.zufar.icedlatte.ai.reviewvalidator.api;

import com.zufar.icedlatte.ai.reviewvalidator.dto.ProductReviewAiValidationResult;
import com.zufar.icedlatte.ai.reviewvalidator.dto.AiResultStatus;
import com.zufar.icedlatte.ai.reviewvalidator.dto.TextModerationResult;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import com.zufar.icedlatte.ai.reviewvalidator.exception.AiServiceConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewAiValidator {

    @Value("${ai.request_body}")
    protected String requestBody;

    @Value("${ai.url}")
    protected String aiServiceUrl;

    @Value("${ai.message_replacement_pattern}")
    protected String messageReplacementPattern;

    private final AiServiceConnectionProvider aiServiceConnectionProvider;

    public ProductReviewAiValidationResult validate(final String productReviewText) {
        HttpURLConnection httpURLConnection = aiServiceConnectionProvider.getConnection();

        try (DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream())) {
            var request = requestBody.replace(messageReplacementPattern, productReviewText);
            dataOutputStream.writeBytes(request);
            dataOutputStream.flush();
        } catch (Exception exception) {
            log.error("Failed to open httpURLConnection", exception);
            throw new AiServiceConnectionException();
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            TextModerationResult result = new Gson().fromJson(response.toString(), TextModerationResult.class);

            AiResultStatus resultStatus;
            if (result.getResults().stream().anyMatch(TextModerationResult.Result::isFlagged)) {
                resultStatus = AiResultStatus.NOT_APPROPRIATE;
            } else {
                resultStatus = AiResultStatus.OK;
            }
            return new ProductReviewAiValidationResult(resultStatus, response.toString());
        } catch (Exception exception) {
            log.error("Failed to validate product's review with AI Checker", exception);
            return new ProductReviewAiValidationResult(AiResultStatus.ERROR, exception.getMessage());
        }
    }
}
