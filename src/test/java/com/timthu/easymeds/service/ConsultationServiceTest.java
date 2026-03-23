package com.timthu.easymeds.service;

import com.timthu.easymeds.config.AppProperties;
import com.timthu.easymeds.domain.ConsultationPlan;
import com.timthu.easymeds.domain.ConsultationSession;
import com.timthu.easymeds.domain.DiagnosisItem;
import com.timthu.easymeds.domain.FinalResult;
import com.timthu.easymeds.domain.GraphContext;
import com.timthu.easymeds.domain.UserInputAnalysis;
import com.timthu.easymeds.domain.UserIntent;
import com.timthu.easymeds.repository.InMemoryConsultationSessionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConsultationServiceTest {

    @Test
    void shouldCreateConsultationAndAskFollowUp() {
        AppProperties properties = new AppProperties();
        properties.getConsultation().setMaxQuestionTurns(5);

        ConsultationAiService aiService = new ConsultationAiService() {
            @Override
            public UserInputAnalysis analyzeUserInput(ConsultationSession session, String message) {
                return new UserInputAnalysis(UserIntent.SYMPTOM_STATEMENT, List.of("咳嗽"), List.of(), List.of(), false);
            }

            @Override
            public ConsultationPlan planNextStep(ConsultationSession session, GraphContext graphContext, boolean forceComplete) {
                return ConsultationPlan.ask("请问您是否发热？", "发热");
            }
        };

        KnowledgeGraphGateway graphGateway = (confirmedSymptoms, deniedSymptoms) -> GraphContext.empty(false);
        ConsultationService service = new ConsultationService(
                new InMemoryConsultationSessionRepository(),
                aiService,
                graphGateway,
                properties
        );

        ConsultationSession session = service.createConsultation("我一直咳嗽");

        Assertions.assertEquals(1, session.getTurnCount());
        Assertions.assertTrue(session.getConfirmedSymptoms().contains("咳嗽"));
        Assertions.assertEquals("发热", session.getPendingSymptom());
        Assertions.assertEquals("请问您是否发热？", session.getLastAssistantMessage().content());
    }

    @Test
    void shouldCompleteConsultationWhenFinished() {
        AppProperties properties = new AppProperties();
        ConsultationAiService aiService = new ConsultationAiService() {
            @Override
            public UserInputAnalysis analyzeUserInput(ConsultationSession session, String message) {
                return new UserInputAnalysis(UserIntent.FINISH, List.of("头痛"), List.of(), List.of(), true);
            }

            @Override
            public ConsultationPlan planNextStep(ConsultationSession session, GraphContext graphContext, boolean forceComplete) {
                return ConsultationPlan.complete(new FinalResult(
                        List.of(new DiagnosisItem("普通感冒", "MEDIUM")),
                        "建议观察并就医。",
                        "遵医嘱用药。",
                        "如加重请线下就诊。"
                ));
            }
        };

        KnowledgeGraphGateway graphGateway = (confirmedSymptoms, deniedSymptoms) -> GraphContext.empty(false);
        ConsultationService service = new ConsultationService(
                new InMemoryConsultationSessionRepository(),
                aiService,
                graphGateway,
                properties
        );

        ConsultationSession session = service.createConsultation("先这样");

        Assertions.assertNotNull(session.getFinalResult());
        Assertions.assertEquals("COMPLETED", session.getStatus().name());
        Assertions.assertEquals("普通感冒", session.getFinalResult().diagnoses().getFirst().name());
    }
}