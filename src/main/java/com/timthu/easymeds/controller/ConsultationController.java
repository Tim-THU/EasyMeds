package com.timthu.easymeds.controller;

import com.timthu.easymeds.api.ApiResponse;
import com.timthu.easymeds.api.ConsultationResponseMapper;
import com.timthu.easymeds.api.dto.ConsultationDetailResponse;
import com.timthu.easymeds.api.dto.ConsultationMessageRequest;
import com.timthu.easymeds.api.dto.ConsultationTurnResponse;
import com.timthu.easymeds.api.dto.FinishConsultationRequest;
import com.timthu.easymeds.api.dto.StartConsultationRequest;
import com.timthu.easymeds.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationResponseMapper responseMapper;

    public ConsultationController(ConsultationService consultationService, ConsultationResponseMapper responseMapper) {
        this.consultationService = consultationService;
        this.responseMapper = responseMapper;
    }

    @PostMapping("/consultations")
    public ApiResponse<ConsultationTurnResponse> create(@Valid @RequestBody StartConsultationRequest request) {
        return ApiResponse.ok(responseMapper.toTurnResponse(consultationService.createConsultation(request.message())));
    }

    @PostMapping("/consultations/{sessionId}/messages")
    public ApiResponse<ConsultationTurnResponse> reply(
            @PathVariable String sessionId,
            @Valid @RequestBody ConsultationMessageRequest request
    ) {
        return ApiResponse.ok(responseMapper.toTurnResponse(consultationService.reply(sessionId, request.message())));
    }

    @GetMapping("/consultations/{sessionId}")
    public ApiResponse<ConsultationDetailResponse> detail(@PathVariable String sessionId) {
        return ApiResponse.ok(responseMapper.toDetailResponse(consultationService.getConsultation(sessionId)));
    }

    @PostMapping("/consultations/{sessionId}/finish")
    public ApiResponse<ConsultationTurnResponse> finish(
            @PathVariable String sessionId,
            @RequestBody(required = false) FinishConsultationRequest request
    ) {
        return ApiResponse.ok(responseMapper.toTurnResponse(consultationService.finish(sessionId)));
    }
}