package com.expensetracker.ai;

import com.expensetracker.exception.ApiExceptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin client around the Google Gemini generateContent REST endpoint.
 * The API key lives ONLY here on the backend — it is read from an environment
 * variable and is never sent to, or reachable from, the React frontend.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(@Value("${app.gemini.api-key:}") String apiKey,
                         @Value("${app.gemini.model:gemini-2.0-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String generateInsight(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiExceptions.AiServiceException("Gemini API key not configured", null);
        }

        String url = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 500
                )
        );

        try {
            String responseJson = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode()) {
                throw new ApiExceptions.AiServiceException("Unexpected Gemini response shape", null);
            }
            return textNode.asText();
        } catch (ApiExceptions.AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new ApiExceptions.AiServiceException("Gemini API call failed", e);
        }
    }
}
