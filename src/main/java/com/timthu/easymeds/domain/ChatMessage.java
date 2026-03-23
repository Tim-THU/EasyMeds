package com.timthu.easymeds.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ChatMessage(String role, String content, AssistantMessageType type, OffsetDateTime createdAt) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static ChatMessage assistant(String content, AssistantMessageType type) {
        return new ChatMessage("assistant", content, type, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
