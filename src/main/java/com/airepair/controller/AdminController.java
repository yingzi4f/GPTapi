package com.airepair.controller;

import com.airepair.entity.ChatMessage;
import com.airepair.entity.Role;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.ChatMessageRepository;
import com.airepair.repository.TeamRepository;
import com.airepair.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/chat-history/all")
    public ResponseEntity<?> getAllTeamChatHistory(@RequestParam String username) {
        try {
            log.info("Getting all team chat history, requested by: {}", username);
            
            // 1. 验证管理员权限
            User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
                
            if (admin.getRole() != Role.ADMIN) {
                throw new RuntimeException("只有管理员可以访问此接口");
            }

            // 2. 获取所有团队消息
            List<ChatMessage> allMessages = chatMessageRepository.findAllTeamMessages();
            
            // 3. 按团队分组
            Map<String, List<ChatMessage>> messagesByTeam = new HashMap<>();
            for (ChatMessage message : allMessages) {
                String sessionId = message.getSessionId();
                messagesByTeam.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
            }
            
            // 4. 构建响应
            List<Map<String, Object>> response = new ArrayList<>();
            for (Map.Entry<String, List<ChatMessage>> entry : messagesByTeam.entrySet()) {
                String sessionId = entry.getKey();
                List<ChatMessage> messages = entry.getValue();
                
                Team team = teamRepository.findBySessionId(sessionId)
                    .orElse(null);
                    
                if (team != null) {
                    Map<String, Object> teamHistory = new HashMap<>();
                    teamHistory.put("teamId", team.getId());
                    teamHistory.put("teamName", team.getName());
                    teamHistory.put("sessionId", sessionId);
                    teamHistory.put("messages", messages);
                    response.add(teamHistory);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting team chat history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/chat-history/clear-all")
    public ResponseEntity<?> clearAllTeamChatHistory(@RequestParam String username) {
        try {
            log.info("Clearing all team chat history, requested by: {}", username);
            
            // 1. 验证管理员权限
            User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
                
            if (admin.getRole() != Role.ADMIN) {
                throw new RuntimeException("只有管理员可以访问此接口");
            }

            // 2. 清空所有团队聊天记录
            chatMessageRepository.deleteAllTeamMessages();
            
            return ResponseEntity.ok(Collections.singletonMap("message", "所有团队的聊天记录已清空"));
        } catch (Exception e) {
            log.error("Error clearing all team chat history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/chat-history/clear/{teamId}")
    public ResponseEntity<?> clearTeamChatHistory(
            @PathVariable Long teamId,
            @RequestParam String username) {
        try {
            log.info("Clearing chat history for team: {}, requested by: {}", teamId, username);
            
            // 1. 验证管理员权限
            User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
                
            if (admin.getRole() != Role.ADMIN) {
                throw new RuntimeException("只有管理员可以访问此接口");
            }

            // 2. 验证团队是否存在
            Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("团队不存在"));

            // 3. 清空指定团队的聊天记录
            chatMessageRepository.deleteBySessionId(team.getSessionId());
            
            return ResponseEntity.ok(Collections.singletonMap("message", 
                String.format("团队 '%s' 的聊天记录已清空", team.getName())));
        } catch (Exception e) {
            log.error("Error clearing team chat history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
