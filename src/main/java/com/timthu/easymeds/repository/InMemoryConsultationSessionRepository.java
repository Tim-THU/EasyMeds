package com.timthu.easymeds.repository;

import com.timthu.easymeds.domain.ConsultationSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryConsultationSessionRepository implements ConsultationSessionRepository {

    private final ConcurrentMap<String, ConsultationSession> storage = new ConcurrentHashMap<>();

    @Override
    public ConsultationSession save(ConsultationSession session) {
        storage.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public Optional<ConsultationSession> findById(String sessionId) {
        return Optional.ofNullable(storage.get(sessionId));
    }
}
