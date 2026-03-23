package com.timthu.easymeds.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConsultationSession {

    private final String sessionId;
    private ConsultationStatus status = ConsultationStatus.QUESTIONING;
    private int turnCount;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Set<String> confirmedSymptoms = new LinkedHashSet<>();
    private final Set<String> deniedSymptoms = new LinkedHashSet<>();
    private final Set<String> unknownSymptoms = new LinkedHashSet<>();
    private String pendingSymptom;
    private FinalResult finalResult;

    public ConsultationSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultationStatus status) {
        this.status = status;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void increaseTurnCount() {
        this.turnCount++;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public Set<String> getConfirmedSymptoms() {
        return confirmedSymptoms;
    }

    public Set<String> getDeniedSymptoms() {
        return deniedSymptoms;
    }

    public Set<String> getUnknownSymptoms() {
        return unknownSymptoms;
    }

    public String getPendingSymptom() {
        return pendingSymptom;
    }

    public void setPendingSymptom(String pendingSymptom) {
        this.pendingSymptom = pendingSymptom;
    }

    public FinalResult getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(FinalResult finalResult) {
        this.finalResult = finalResult;
    }

    public void addUserMessage(String content) {
        messages.add(ChatMessage.user(content));
        increaseTurnCount();
    }

    public void addAssistantMessage(String content, AssistantMessageType type) {
        messages.add(ChatMessage.assistant(content, type));
    }

    public ChatMessage getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if ("assistant".equals(message.role())) {
                return message;
            }
        }
        return null;
    }
}
