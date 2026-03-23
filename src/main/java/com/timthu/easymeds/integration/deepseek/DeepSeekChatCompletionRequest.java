package com.timthu.easymeds.integration.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeepSeekChatCompletionRequest(
        String model,
        List<DeepSeekChatMessage> messages,
        @JsonProperty("response_format") DeepSeekResponseFormat responseFormat,
        @JsonProperty("max_tokens") Integer maxTokens,
        Boolean stream
) {
}
