package com.airepair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.airepair.entity.Model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class QwenService {
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    @Autowired
    private ModelService modelService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public interface StreamCallback {
        void onMessage(String message);
        void onComplete();
        void onError(Throwable t);
    }

    public void streamChat(String modelName, List<Object> messages, StreamCallback callback) {
        try {
            // 获取模型及API-KEY
            Model model = modelService.getAllModels().stream()
                    .filter(m -> m.getName().equals(modelName))
                    .findFirst().orElseThrow(() -> new RuntimeException("模型未配置: " + modelName));
            String apiKey = model.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) throw new RuntimeException("API-KEY未配置");

            // 构造请求体
            String requestJson = objectMapper.writeValueAsString(new QwenRequest(modelName, messages));
            log.debug("Qwen请求体: {}", requestJson);

            URL url = new URL(QWEN_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.getOutputStream().write(requestJson.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().flush();
            conn.getOutputStream().close();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    // 解析响应
                    JsonNode root = objectMapper.readTree(responseBuilder.toString());
                    String content = root.at("/output/text").asText("");
                    callback.onMessage(content);
                    callback.onComplete();
                }
            } else {
                String errorMessage = "Qwen API响应错误: " + conn.getResponseCode();
                log.error(errorMessage);
                callback.onError(new RuntimeException(errorMessage));
            }
        } catch (Exception e) {
            log.error("Qwen对话调用异常", e);
            callback.onError(e);
        }
    }

    // Qwen请求体内部类
    public static class QwenRequest {
        public String model;
        public Input input;
        public QwenRequest(String model, List<Object> messages) {
            this.model = model;
            this.input = new Input(messages);
        }
        public static class Input {
            public List<Object> messages;
            public Input(List<Object> messages) { this.messages = messages; }
        }
    }
} 