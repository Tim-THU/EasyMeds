package com.timthu.easymeds.api.dto;

import java.util.List;

public record FinalResultResponse(
        List<DiagnosisItemResponse> diagnoses,
        String medicalAdvice,
        String medicationAdvice,
        String riskNotice
) {
}