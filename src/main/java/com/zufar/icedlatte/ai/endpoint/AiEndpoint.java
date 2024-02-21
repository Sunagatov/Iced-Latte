package com.zufar.icedlatte.ai.endpoint;

import com.zufar.icedlatte.ai.api.AiApi;
import com.zufar.icedlatte.ai.dto.AiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = AiEndpoint.AI_URL)
public class AiEndpoint {

    public static final String AI_URL = "/api/v1/ai";

    private final AiApi aiApi;
    @PostMapping(value = "/check")
    public ResponseEntity<AiDto> check(@RequestBody final String request) {
        log.warn("Received the request to check");
        var response = aiApi.checkMessage(request);
        log.info("Message was checked with result={}", response);
        return ResponseEntity.ok()
                .body(response);
    }

}
