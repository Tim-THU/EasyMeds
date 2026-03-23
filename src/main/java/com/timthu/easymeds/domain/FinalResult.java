package com.timthu.easymeds.domain;

import java.util.List;

public record FinalResult(
        List<DiagnosisItem> diagnoses,
        String medicalAdvice,
        String medicationAdvice,
        String riskNotice
) {
}
