package com.timthu.easymeds.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultationMessageRequest(@NotBlank(message = "不能为空") String message) {
}