package com.zufar.icedlatte.ai.api;

import com.zufar.icedlatte.ai.dto.AiDto;

public interface AiApi {

    AiDto checkMessage(final String message);
}
