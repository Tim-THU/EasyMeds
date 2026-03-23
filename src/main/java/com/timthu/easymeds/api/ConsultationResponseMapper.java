package com.timthu.easymeds.api;

import com.timthu.easymeds.api.dto.ConsultationDetailResponse;
import com.timthu.easymeds.api.dto.ConsultationTurnResponse;
import com.timthu.easymeds.api.dto.DiagnosisItemResponse;
import com.timthu.easymeds.api.dto.FinalResultResponse;
import com.timthu.easymeds.api.dto.MessageResponse;
import com.timthu.easymeds.domain.ChatMessage;
import com.timthu.easymeds.domain.ConsultationSession;
import com.timthu.easymeds.domain.DiagnosisItem;
import com.timthu.easymeds.domain.FinalResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConsultationResponseMapper {

    public ConsultationTurnResponse toTurnResponse(ConsultationSession session) {
        ChatMessage assistantMessage = session.getLastAssistantMessage();
        return new ConsultationTurnResponse(
                session.getSessionId(),
                session.getStatus().name(),
                session.getTurnCount(),
                List.copyOf(session.getConfirmedSymptoms()),
                assistantMessage == null ? null : toMessageResponse(assistantMessage),
                toFinalResultResponse(session.getFinalResult())
        );
    }

    public ConsultationDetailResponse toDetailResponse(ConsultationSession session) {
        return new ConsultationDetailResponse(
                session.getSessionId(),
                session.getStatus().name(),
                session.getTurnCount(),
                List.copyOf(session.getConfirmedSymptoms()),
                session.getMessages().stream().map(this::toMessageResponse).toList(),
                toFinalResultResponse(session.getFinalResult())
        );
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.role(),
                message.content(),
                message.type() == null ? null : message.type().name(),
                message.createdAt()
        );
    }

    private FinalResultResponse toFinalResultResponse(FinalResult finalResult) {
        if (finalResult == null) {
            return null;
        }
        return new FinalResultResponse(
                finalResult.diagnoses().stream().map(this::toDiagnosisItemResponse).toList(),
                finalResult.medicalAdvice(),
                finalResult.medicationAdvice(),
                finalResult.riskNotice()
        );
    }

    private DiagnosisItemResponse toDiagnosisItemResponse(DiagnosisItem item) {
        return new DiagnosisItemResponse(item.name(), item.confidence());
    }
}