package com.timthu.easymeds.integration.deepseek;

import com.timthu.easymeds.config.AppProperties;
import com.timthu.easymeds.exception.ApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class DeepSeekClient {

    private final WebClient webClient;
    private final AppProperties appProperties;

    public DeepSeekClient(WebClient deepSeekWebClient, AppProperties appProperties) {
        this.webClient = deepSeekWebClient;
        this.appProperties = appProperties;
    }

    public String requestJson(String model, String systemPrompt, String userPrompt, int maxTokens) {
        if (appProperties.getDeepseek().getApiKey() == null || appProperties.getDeepseek().getApiKey().isBlank()) {
            throw new ApiException("MODEL_SERVICE_ERROR", "DeepSeek API Key 未配置", HttpStatus.BAD_GATEWAY);
        }

        DeepSeekChatCompletionRequest request = new DeepSeekChatCompletionRequest(
                model,
                List.of(
                        new DeepSeekChatMessage("system", systemPrompt),
                        new DeepSeekChatMessage("user", userPrompt)
                ),
                DeepSeekResponseFormat.jsonObject(),
                maxTokens,
                false
        );

        DeepSeekChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getDeepseek().getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekChatCompletionResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ApiException("MODEL_SERVICE_ERROR", "DeepSeek 返回为空", HttpStatus.BAD_GATEWAY);
        }

        String content = response.choices().getFirst().message().content();
        if (content == null || content.isBlank()) {
            throw new ApiException("MODEL_SERVICE_ERROR", "DeepSeek 返回了空内容", HttpStatus.BAD_GATEWAY);
        }
        return content;
    }
}