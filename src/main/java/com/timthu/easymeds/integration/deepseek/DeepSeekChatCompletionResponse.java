package com.timthu.easymeds.integration.deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeepSeekChatCompletionResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String content,
            @JsonProperty("reasoning_content") String reasoningContent
    ) {
    }
}
