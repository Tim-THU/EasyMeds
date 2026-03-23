package com.timthu.easymeds.api.dto;

import java.util.List;

public record ConsultationTurnResponse(
        String sessionId,
        String status,
        int turnCount,
        List<String> confirmedSymptoms,
        MessageResponse assistantMessage,
        FinalResultResponse finalResult
) {
}