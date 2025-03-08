package com.airepair.service;

import com.airepair.dto.OllamaChatRequest;
import com.airepair.dto.OllamaChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OllamaService {
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public interface StreamCallback {
        void onMessage(String message);
        void onComplete();
        void onError(Throwable t);
    }

    public List<String> getLocalModels() {
        try {
            log.info("Getting local models from Ollama");
            URL url = new URL(ollamaApiUrl + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    
                    String response = reader.lines().reduce("", String::concat);
                    JsonNode root = objectMapper.readTree(response);
                    List<String> models = new ArrayList<>();
                    
                    if (root.has("models")) {
                        root.get("models").forEach(model -> {
                            if (model.has("name")) {
                                models.add(model.get("name").asText());
                            }
                        });
                    }
                    
                    log.info("Found {} local models", models.size());
                    log.debug("Local models: {}", models);
                    return models;
                }
            } else {
                String errorMessage = "Error response from Ollama API: " + conn.getResponseCode();
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception e) {
            log.error("Error getting local models", e);
            throw new RuntimeException("Failed to get local models: " + e.getMessage());
        }
    }

    public void streamChat(String modelName, List<OllamaChatRequest.Message> messages, StreamCallback callback) {
        try {
            log.info("Starting chat stream with model: {}", modelName);
            
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(modelName);
            request.setMessages(messages);
            request.setStream(true);

            String requestJson = objectMapper.writeValueAsString(request);
            log.debug("Request JSON: {}", requestJson);

            URL url = new URL(ollamaApiUrl + "/api/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // 发送请求
            conn.getOutputStream().write(requestJson.getBytes("UTF-8"));
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            log.debug("Request sent");

            // 读取响应
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            OllamaChatResponse response = objectMapper.readValue(line, OllamaChatResponse.class);
                            if (response.getMessage() != null && response.getMessage().getContent() != null) {
                                log.debug("Received message: {}", response.getMessage().getContent());
                                callback.onMessage(response.getMessage().getContent());
                            }
                            if (response.getDone() != null && response.getDone()) {
                                log.debug("Stream completed");
                                callback.onComplete();
                                break;
                            }
                        } catch (Exception e) {
                            log.error("Error parsing response line: {}", line, e);
                            callback.onError(e);
                            break;
                        }
                    }
                }
            } else {
                String errorMessage = "Error response from Ollama API: " + conn.getResponseCode();
                log.error(errorMessage);
                callback.onError(new RuntimeException(errorMessage));
            }
        } catch (Exception e) {
            log.error("Error in stream chat", e);
            callback.onError(e);
        }
    }
}
