package com.airepair.controller;

import com.airepair.dto.ChatRequest;
import com.airepair.dto.OllamaChatRequest;
import com.airepair.entity.ChatMessage;
import com.airepair.repository.ChatMessageRepository;
import com.airepair.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatMessageRepository chatMessageRepository;
    private final OllamaService ollamaService;

    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getChatHistory(@PathVariable String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<?> clearChatHistory(@PathVariable String sessionId) {
        log.info("Clearing chat history for session: {}", sessionId);
        
        long messageCount = chatMessageRepository.countBySessionId(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("deletedMessages", messageCount);
        response.put("message", "Chat history cleared successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/clear-history")
    public ResponseEntity<?> clearChatHistoryPost(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Session ID is required"
            ));
        }
        
        log.info("Clearing chat history for session: {}", sessionId);
        
        long messageCount = chatMessageRepository.countBySessionId(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("deletedMessages", messageCount);
        response.put("message", "Chat history cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatGet(
            @RequestParam String sessionId,
            @RequestParam String modelName,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "anonymous") String username) {
        ChatRequest request = new ChatRequest();
        request.setSessionId(sessionId);
        request.setModelName(modelName);
        request.setContent(content);
        request.setUsername(username);
        return handleStreamChat(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatPost(@RequestBody ChatRequest request) {
        return handleStreamChat(request);
    }

    private SseEmitter handleStreamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter();
        
        // 如果 sessionId 为空，生成新的
        if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }
        
        // 获取历史消息
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(request.getSessionId());
        
        // 保存用户消息到数据库
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(request.getSessionId());
        userMessage.setRole("user");
        userMessage.setContent(request.getContent());
        userMessage.setUsername(request.getUsername());
        userMessage.setModelName(request.getModelName());
        chatMessageRepository.save(userMessage);

        // 创建AI回复消息
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(request.getSessionId());
        aiMessage.setRole("assistant");
        aiMessage.setContent("");
        aiMessage.setUsername(request.getUsername());
        aiMessage.setModelName(request.getModelName());
        ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);

        StringBuilder responseBuilder = new StringBuilder();

        // 调用Ollama服务，这里不需要传递用户名和模型名到上下文
        List<OllamaChatRequest.Message> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            messages.add(new OllamaChatRequest.Message(msg.getRole(), msg.getContent()));
        }
        messages.add(new OllamaChatRequest.Message("user", request.getContent()));
        
        ollamaService.streamChat(request.getModelName(), messages, new OllamaService.StreamCallback() {
            @Override
            public void onMessage(String message) {
                try {
                    responseBuilder.append(message);
                    emitter.send(message);
                } catch (IOException e) {
                    log.error("Error sending SSE", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onComplete() {
                try {
                    // 更新AI消息内容
                    savedAiMessage.setContent(responseBuilder.toString());
                    chatMessageRepository.save(savedAiMessage);
                    
                    emitter.send(SseEmitter.event()
                        .data(responseBuilder.toString())
                        .id(request.getSessionId())
                        .name("complete")
                        .build());
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Error completing SSE", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in stream chat", t);
                emitter.completeWithError(t);
            }
        });

        return emitter;
    }
}
