package com.timthu.easymeds.service;

import com.timthu.easymeds.domain.ConsultationPlan;
import com.timthu.easymeds.domain.ConsultationSession;
import com.timthu.easymeds.domain.GraphContext;
import com.timthu.easymeds.domain.UserInputAnalysis;

public interface ConsultationAiService {

    UserInputAnalysis analyzeUserInput(ConsultationSession session, String message);

    ConsultationPlan planNextStep(ConsultationSession session, GraphContext graphContext, boolean forceComplete);
}
