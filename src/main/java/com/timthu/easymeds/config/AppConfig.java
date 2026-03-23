package com.timthu.easymeds.config;

import com.timthu.easymeds.repository.ConsultationSessionRepository;
import com.timthu.easymeds.repository.InMemoryConsultationSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public WebClient deepSeekWebClient(AppProperties appProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.getDeepseek().getBaseUrl())
                .build();
    }

    @Bean
    public ConsultationSessionRepository consultationSessionRepository() {
        return new InMemoryConsultationSessionRepository();
    }
}
