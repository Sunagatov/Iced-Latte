package com.zufar.icedlatte.ai.provider;

import com.zufar.icedlatte.ai.api.AiModerationService;
import com.zufar.icedlatte.ai.exception.InappropriateContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class LangChain4jModerationService implements AiModerationService {

    private static final String OK = "OK";

    private final ModerationAiService moderationAiService;

    @Override
    public void moderate(String text) {
        String response = moderationAiService.moderate(text);
        if (!response.startsWith(OK)) {
            String reason = response.contains(":") ? response.substring(response.indexOf(':') + 1).trim() : response;
            throw new InappropriateContentException("Review contains inappropriate content: " + reason);
        }
    }
}
