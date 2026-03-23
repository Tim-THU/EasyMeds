package com.timthu.easymeds.integration.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeepSeekResponseFormat(@JsonProperty("type") String type) {

    public static DeepSeekResponseFormat jsonObject() {
        return new DeepSeekResponseFormat("json_object");
    }
}
