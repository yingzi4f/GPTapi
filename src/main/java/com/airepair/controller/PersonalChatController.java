package com.airepair.controller;

import com.airepair.dto.ChatRequest;
import com.airepair.dto.OllamaChatRequest;
import com.airepair.entity.PersonalMessage;
import com.airepair.entity.PersonalSession;
import com.airepair.entity.User;
import com.airepair.repository.PersonalMessageRepository;
import com.airepair.repository.PersonalSessionRepository;
import com.airepair.repository.UserRepository;
import com.airepair.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/personal-chat")
public class PersonalChatController {
    
    @Autowired
    private PersonalMessageRepository personalMessageRepository;
    
    @Autowired
    private PersonalSessionRepository personalSessionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OllamaService ollamaService;

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestParam String username) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            PersonalSession session = new PersonalSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setUserId(user.getId());
            session.setName("新会话");
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            
            PersonalSession savedSession = personalSessionRepository.save(session);
            return ResponseEntity.ok(savedSession);
        } catch (Exception e) {
            log.error("Error creating session", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{username}")
    public ResponseEntity<?> getUserSessions(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            List<PersonalSession> sessions = personalSessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error getting user sessions", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/sessions/{username}/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String username,
            @PathVariable String sessionId) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            personalMessageRepository.deleteBySessionIdAndUserId(sessionId, user.getId());
            personalSessionRepository.deleteBySessionIdAndUserId(sessionId, user.getId());
            
            return ResponseEntity.ok(Collections.singletonMap("message", "Session deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting session", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/history/{username}/{sessionId}")
    public ResponseEntity<?> getChatHistory(
            @PathVariable String username,
            @PathVariable String sessionId) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            List<PersonalMessage> messages = personalMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, user.getId());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting chat history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter();
        try {
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 验证会话存在性和所有权
            PersonalSession session = personalSessionRepository.findBySessionIdAndUserId(request.getSessionId(), user.getId())
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
            
            // 获取历史消息
            List<PersonalMessage> history = personalMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(request.getSessionId(), user.getId());
            
            // 保存用户消息
            PersonalMessage userMessage = new PersonalMessage();
            userMessage.setSessionId(request.getSessionId());
            userMessage.setUserId(user.getId());
            userMessage.setRole("user");
            userMessage.setContent(request.getContent());
            userMessage.setModelName(request.getModelName());
            personalMessageRepository.save(userMessage);

            // 创建AI回复消息
            PersonalMessage aiMessage = new PersonalMessage();
            aiMessage.setSessionId(request.getSessionId());
            aiMessage.setUserId(user.getId());
            aiMessage.setRole("assistant");
            aiMessage.setContent("");
            aiMessage.setModelName(request.getModelName());
            PersonalMessage savedAiMessage = personalMessageRepository.save(aiMessage);

            // 更新会话信息
            session.setLastMessage(request.getContent());
            session.setModelName(request.getModelName());
            session.setUpdatedAt(LocalDateTime.now());
            personalSessionRepository.save(session);

            StringBuilder responseBuilder = new StringBuilder();

            // 调用Ollama服务
            List<OllamaChatRequest.Message> messages = new ArrayList<>();
            for (PersonalMessage msg : history) {
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
                        personalMessageRepository.save(savedAiMessage);
                        
                        // 更新会话最后消息
                        session.setLastMessage(responseBuilder.toString());
                        session.setUpdatedAt(LocalDateTime.now());
                        personalSessionRepository.save(session);
                        
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
        } catch (Exception e) {
            log.error("Error in chat stream", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PutMapping("/sessions/{username}/{sessionId}/name")
    public ResponseEntity<?> updateSessionName(
            @PathVariable String username,
            @PathVariable String sessionId,
            @RequestParam String name) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            PersonalSession session = personalSessionRepository.findBySessionIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
            
            session.setName(name);
            session.setUpdatedAt(LocalDateTime.now());
            PersonalSession updatedSession = personalSessionRepository.save(session);
            
            return ResponseEntity.ok(updatedSession);
        } catch (Exception e) {
            log.error("Error updating session name", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
