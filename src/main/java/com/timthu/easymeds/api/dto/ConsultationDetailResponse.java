package com.timthu.easymeds.api.dto;

import java.util.List;

public record ConsultationDetailResponse(
        String sessionId,
        String status,
        int turnCount,
        List<String> confirmedSymptoms,
        List<MessageResponse> messages,
        FinalResultResponse finalResult
) {
}