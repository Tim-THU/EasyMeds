package com.timthu.easymeds.controller;

import com.timthu.easymeds.api.ApiResponse;
import com.timthu.easymeds.api.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(new HealthResponse("UP"));
    }
}