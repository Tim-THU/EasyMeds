package com.timthu.easymeds.repository;

import com.timthu.easymeds.domain.ConsultationSession;

import java.util.Optional;

public interface ConsultationSessionRepository {

    ConsultationSession save(ConsultationSession session);

    Optional<ConsultationSession> findById(String sessionId);
}
