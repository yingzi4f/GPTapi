package com.airepair.service;

import com.airepair.dto.OllamaChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class OllamaService {
    private final String OLLAMA_API_URL = "http://127.0.0.1:11434/api";
    private final String DEFAULT_MODEL = "deepseek-coder:6.7b";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Double DEFAULT_TEMPERATURE = 0.7;
    private final Integer DEFAULT_CONTEXT_LENGTH = 4096;

    public interface StreamCallback {
        void onMessage(String message);
        void onComplete();
        void onError(Throwable t);
    }

    public List<String> getLocalModels() {
        try {
            log.info("Fetching local models from Ollama...");
            ResponseEntity<String> response = restTemplate.getForEntity(OLLAMA_API_URL + "/tags", String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to get models, status: " + response.getStatusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.getBody());
            List<String> models = new ArrayList<>();
            
            if (root.has("models")) {
                root.get("models").forEach(model -> {
                    if (model.has("name")) {
                        models.add(model.get("name").asText());
                    }
                });
            }
            
            log.info("Found {} local models: {}", models.size(), models);
            return models;
        } catch (Exception e) {
            log.error("Failed to get local models from Ollama", e);
            return new ArrayList<>();
        }
    }

    public void streamChat(String modelName, List<OllamaChatRequest.Message> messages, StreamCallback callback) {
        log.info("Starting chat stream with model: {}", modelName);
        log.debug("Input messages: {}", messages);
        
        executorService.execute(() -> {
            try {
                // 1. 检查Ollama服务是否可用
                try {
                    log.info("Checking Ollama service availability...");
                    ResponseEntity<String> healthResponse = restTemplate.getForEntity(OLLAMA_API_URL + "/version", String.class);
                    if (!healthResponse.getStatusCode().is2xxSuccessful()) {
                        throw new RuntimeException("Ollama service is not available, status: " + healthResponse.getStatusCode());
                    }
                    log.info("Ollama service is available");
                } catch (Exception e) {
                    log.error("Ollama service health check failed", e);
                    throw new RuntimeException("Failed to connect to Ollama service: " + e.getMessage());
                }

                // 2. 检查模型是否存在
                List<String> availableModels = getLocalModels();
                String finalModelName = modelName;
                if (!availableModels.contains(modelName)) {
                    log.info("Model {} not found in local models list, will try to use it anyway", modelName);
                }

                // 3. 准备请求数据
                StringBuilder prompt = new StringBuilder();
                for (int i = 0; i < messages.size(); i++) {
                    OllamaChatRequest.Message msg = messages.get(i);
                    if ("user".equals(msg.getRole())) {
                        prompt.append("Human: ").append(msg.getContent()).append("\n");
                    } else if ("assistant".equals(msg.getRole())) {
                        prompt.append("Assistant: ").append(msg.getContent()).append("\n");
                    }
                }

                String escapedPrompt = prompt.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

                // 4. 设置请求体
                String requestBody = String.format(
                    "{\"model\":\"%s\",\"prompt\":\"%s\",\"temperature\":%f,\"context_length\":%d,\"stream\":true}",
                    finalModelName, escapedPrompt, DEFAULT_TEMPERATURE, DEFAULT_CONTEXT_LENGTH
                );

                // 5. 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("Accept-Charset", "UTF-8");

                // 6. 创建请求实体
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
                log.info("Sending request to Ollama");
                log.debug("Request body: {}", requestBody);

                // 7. 执行请求
                ResponseEntity<byte[]> response = restTemplate.exchange(
                    OLLAMA_API_URL + "/generate",
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
                );

                log.info("Ollama response code: {}", response.getStatusCode());
                log.info("Starting to read response stream...");

                // 8. 处理响应
                if (response.getBody() != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(new java.io.ByteArrayInputStream(response.getBody()), "UTF-8"))) {
                        
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.trim().isEmpty()) {
                                log.debug("Received response line: {}", line);
                                JsonNode node = objectMapper.readTree(line);
                                
                                if (node.has("response")) {
                                    String content = node.get("response").asText();
                                    if (!content.isEmpty()) {
                                        callback.onMessage(content);
                                        log.debug("Sending message to client: {}", content);
                                    }
                                }
                                
                                if (node.has("done") && node.get("done").asBoolean()) {
                                    log.info("Received completion signal from Ollama");
                                    break;
                                }
                            }
                        }
                    }
                }

                log.info("Chat stream completed successfully");
                callback.onComplete();
            } catch (Exception e) {
                log.error("Error in stream chat", e);
                callback.onError(e);
            }
        });
    }
}
