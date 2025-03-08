package com.airepair.controller;

import com.airepair.dto.ChatRequest;
import com.airepair.dto.OllamaChatRequest;
import com.airepair.entity.ChatMessage;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.ChatMessageRepository;
import com.airepair.repository.TeamRepository;
import com.airepair.repository.UserRepository;
import com.airepair.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/team-chat")
@CrossOrigin
public class TeamChatController {

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getChatHistory(@PathVariable String sessionId) {
        try {
            log.info("Getting chat history for session: {}", sessionId);
            return chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId);
        } catch (Exception e) {
            log.error("Error getting chat history", e);
            return Collections.emptyList();
        }
    }

    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<?> clearTeamChatHistory(@PathVariable String sessionId, @RequestParam String username) {
        try {
            log.info("Clearing team chat history for session: {}, requested by: {}", sessionId, username);
            
            // 1. 验证用户
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (user.getTeamId() == null) {
                throw new RuntimeException("用户不属于任何团队");
            }

            // 2. 验证团队和会话
            Team team = teamRepository.findById(user.getTeamId())
                .orElseThrow(() -> new RuntimeException("团队不存在"));

            if (!team.getSessionId().equals(sessionId)) {
                throw new RuntimeException("无效的会话ID");
            }

            // 3. 验证用户是否为组长
            if (!team.getLeaderId().equals(user.getId())) {
                throw new RuntimeException("只有组长可以清空聊天记录");
            }

            // 4. 清空聊天记录
            chatMessageRepository.deleteBySessionId(sessionId);
            
            return ResponseEntity.ok(Collections.singletonMap("message", "聊天记录已清空"));
        } catch (Exception e) {
            log.error("Error clearing team chat history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        log.info("Received chat stream request for session: {}", request.getSessionId());
        
        SseEmitter emitter = new SseEmitter(-1L); // 设置无限超时
        
        try {
            // 1. 验证用户和会话
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (user.getTeamId() == null) {
                throw new RuntimeException("用户不属于任何团队");
            }

            Team team = teamRepository.findById(user.getTeamId())
                .orElseThrow(() -> new RuntimeException("团队不存在"));

            if (!team.getSessionId().equals(request.getSessionId())) {
                throw new RuntimeException("无效的会话ID");
            }

            // 2. 保存用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSessionId(request.getSessionId());
            userMessage.setUserId(user.getId());
            userMessage.setRole("user");
            userMessage.setContent(request.getContent());
            userMessage.setUsername(request.getUsername());
            userMessage.setModelName(request.getModelName());
            chatMessageRepository.save(userMessage);

            // 3. 获取历史消息
            List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAt(request.getSessionId());
            List<OllamaChatRequest.Message> messages = new ArrayList<>();
            
            // 添加系统提示
            messages.add(new OllamaChatRequest.Message("system", 
                "你是一个专业的AI助手，请提供准确和有帮助的回答。请使用中文回复。"));
            
            // 添加历史消息
            for (ChatMessage msg : history) {
                messages.add(new OllamaChatRequest.Message(msg.getRole(), msg.getContent()));
            }

            // 4. 调用Ollama服务
            ollamaService.streamChat(
                request.getModelName(),
                messages,
                new OllamaService.StreamCallback() {
                    private final StringBuilder responseBuilder = new StringBuilder();

                    @Override
                    public void onMessage(String message) {
                        try {
                            responseBuilder.append(message);
                            emitter.send(message);
                        } catch (IOException e) {
                            log.error("Error sending message", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            // 保存AI回复
                            ChatMessage aiMessage = new ChatMessage();
                            aiMessage.setSessionId(request.getSessionId());
                            aiMessage.setUserId(user.getId());
                            aiMessage.setRole("assistant");
                            aiMessage.setContent(responseBuilder.toString());
                            aiMessage.setUsername("AI");
                            aiMessage.setModelName(request.getModelName());
                            chatMessageRepository.save(aiMessage);
                            
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("Error completing stream", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("Error in stream", t);
                        emitter.completeWithError(t);
                    }
                }
            );
        } catch (Exception e) {
            log.error("Error setting up chat stream", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
