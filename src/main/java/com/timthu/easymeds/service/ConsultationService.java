package com.timthu.easymeds.service;

import com.timthu.easymeds.config.AppProperties;
import com.timthu.easymeds.domain.AssistantMessageType;
import com.timthu.easymeds.domain.ConsultationPlan;
import com.timthu.easymeds.domain.ConsultationSession;
import com.timthu.easymeds.domain.ConsultationStatus;
import com.timthu.easymeds.domain.DecisionAction;
import com.timthu.easymeds.domain.GraphContext;
import com.timthu.easymeds.domain.UserInputAnalysis;
import com.timthu.easymeds.exception.ApiException;
import com.timthu.easymeds.repository.ConsultationSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConsultationService {

    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationAiService consultationAiService;
    private final KnowledgeGraphGateway knowledgeGraphGateway;
    private final AppProperties appProperties;

    public ConsultationService(
            ConsultationSessionRepository sessionRepository,
            ConsultationAiService consultationAiService,
            KnowledgeGraphGateway knowledgeGraphGateway,
            AppProperties appProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.consultationAiService = consultationAiService;
        this.knowledgeGraphGateway = knowledgeGraphGateway;
        this.appProperties = appProperties;
    }

    public ConsultationSession createConsultation(String message) {
        ConsultationSession session = new ConsultationSession(UUID.randomUUID().toString());
        session.addUserMessage(message);
        applyAnalysis(session, consultationAiService.analyzeUserInput(session, message));
        continueOrComplete(session, false);
        return sessionRepository.save(session);
    }

    public ConsultationSession reply(String sessionId, String message) {
        ConsultationSession session = getRequiredSession(sessionId);
        ensureSessionOpen(session);
        session.addUserMessage(message);
        UserInputAnalysis analysis = consultationAiService.analyzeUserInput(session, message);
        applyAnalysis(session, analysis);
        continueOrComplete(session, analysis.shouldFinish());
        return sessionRepository.save(session);
    }

    public ConsultationSession getConsultation(String sessionId) {
        return getRequiredSession(sessionId);
    }

    public ConsultationSession finish(String sessionId) {
        ConsultationSession session = getRequiredSession(sessionId);
        ensureSessionOpen(session);
        completeSession(session);
        return sessionRepository.save(session);
    }

    private void continueOrComplete(ConsultationSession session, boolean shouldFinish) {
        if (shouldFinish || session.getTurnCount() >= appProperties.getConsultation().getMaxQuestionTurns()) {
            completeSession(session);
            return;
        }

        GraphContext graphContext = knowledgeGraphGateway.searchContext(
                session.getConfirmedSymptoms(),
                session.getDeniedSymptoms()
        );
        ConsultationPlan plan = consultationAiService.planNextStep(session, graphContext, false);

        if (plan.action() == DecisionAction.ASK_FOLLOW_UP && hasText(plan.question())) {
            session.setPendingSymptom(normalize(plan.targetSymptom()));
            session.addAssistantMessage(plan.question(), AssistantMessageType.FOLLOW_UP);
            return;
        }

        completeSession(session);
    }

    private void completeSession(ConsultationSession session) {
        GraphContext graphContext = knowledgeGraphGateway.searchContext(
                session.getConfirmedSymptoms(),
                session.getDeniedSymptoms()
        );
        ConsultationPlan plan = consultationAiService.planNextStep(session, graphContext, true);
        session.setPendingSymptom(null);
        session.setFinalResult(plan.finalResult());
        session.setStatus(ConsultationStatus.COMPLETED);
        session.addAssistantMessage("问诊已结束，以下是基于当前信息的初步总结。", AssistantMessageType.FINAL_SUMMARY);
    }

    private ConsultationSession getRequiredSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "会话不存在", HttpStatus.NOT_FOUND));
    }

    private void ensureSessionOpen(ConsultationSession session) {
        if (session.getStatus() == ConsultationStatus.COMPLETED) {
            throw new ApiException("SESSION_COMPLETED", "会话已结束，不能继续发送消息", HttpStatus.CONFLICT);
        }
    }

    private void applyAnalysis(ConsultationSession session, UserInputAnalysis analysis) {
        mergeSymptoms(session.getConfirmedSymptoms(), session.getDeniedSymptoms(), session.getUnknownSymptoms(), analysis.presentSymptoms(), session);
        mergeSymptoms(session.getDeniedSymptoms(), session.getConfirmedSymptoms(), session.getUnknownSymptoms(), analysis.absentSymptoms(), session);
        mergeSymptoms(session.getUnknownSymptoms(), session.getConfirmedSymptoms(), session.getDeniedSymptoms(), analysis.unknownSymptoms(), session);
    }

    private void mergeSymptoms(
            Set<String> primary,
            Set<String> conflictA,
            Set<String> conflictB,
            List<String> symptoms,
            ConsultationSession session
    ) {
        for (String symptom : symptoms) {
            String normalized = normalize(symptom);
            if (normalized == null) {
                continue;
            }
            primary.add(normalized);
            conflictA.remove(normalized);
            conflictB.remove(normalized);
            if (normalized.equals(session.getPendingSymptom()) && primary != session.getUnknownSymptoms()) {
                session.setPendingSymptom(null);
            }
        }
    }

    private String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}