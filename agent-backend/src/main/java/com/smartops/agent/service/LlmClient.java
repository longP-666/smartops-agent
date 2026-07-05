package com.smartops.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LlmClient {
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    public LlmClient(AgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (!properties.getAi().isEnabled() || properties.getAi().getApiKey() == null || properties.getAi().getApiKey().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", properties.getAi().getChatModel(),
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getAi().getBaseUrl().replaceAll("/$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getAi().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getAi().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }
}
