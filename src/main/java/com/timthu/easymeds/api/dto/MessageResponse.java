package com.timthu.easymeds.api.dto;

import java.time.OffsetDateTime;

public record MessageResponse(String role, String content, String type, OffsetDateTime createdAt) {
}