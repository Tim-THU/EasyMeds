package com.timthu.easymeds.domain;

public record ConsultationPlan(
        DecisionAction action,
        String question,
        String targetSymptom,
        FinalResult finalResult
) {

    public static ConsultationPlan ask(String question, String targetSymptom) {
        return new ConsultationPlan(DecisionAction.ASK_FOLLOW_UP, question, targetSymptom, null);
    }

    public static ConsultationPlan complete(FinalResult finalResult) {
        return new ConsultationPlan(DecisionAction.COMPLETE, null, null, finalResult);
    }
}
