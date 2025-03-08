package com.airepair.controller;

import com.airepair.entity.ChatMessage;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.ChatMessageRepository;
import com.airepair.repository.TeamRepository;
import com.airepair.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/team-sessions")
@CrossOrigin
public class TeamSessionController {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/{sessionId}/history")
    public ResponseEntity<?> getSessionHistory(
            @PathVariable String sessionId,
            @RequestParam String username) {
        try {
            log.info("Getting chat history for session: {} by user: {}", sessionId, username);
            
            // 1. 验证用户
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 2. 验证用户是否属于该团队
            if (user.getTeamId() == null) {
                throw new RuntimeException("用户不属于任何团队");
            }

            Team team = teamRepository.findById(user.getTeamId())
                .orElseThrow(() -> new RuntimeException("团队不存在"));

            // 3. 验证会话ID是否属于该团队
            if (!team.getSessionId().equals(sessionId)) {
                throw new RuntimeException("无效的会话ID");
            }

            // 4. 获取会话历史
            List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("teamName", team.getName());
            response.put("sessionId", sessionId);
            response.put("messages", history);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting session history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}/history")
    public ResponseEntity<?> clearSessionHistory(
            @PathVariable String sessionId,
            @RequestParam String username) {
        try {
            log.info("Clearing chat history for session: {} by user: {}", sessionId, username);
            
            // 1. 验证用户
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 2. 验证用户是否为管理员
            if (user.getRole() != com.airepair.entity.Role.ADMIN) {
                throw new RuntimeException("只有管理员可以清空会话历史");
            }

            // 3. 验证用户是否属于该团队
            if (user.getTeamId() == null) {
                throw new RuntimeException("用户不属于任何团队");
            }

            Team team = teamRepository.findById(user.getTeamId())
                .orElseThrow(() -> new RuntimeException("团队不存在"));

            // 4. 验证会话ID是否属于该团队
            if (!team.getSessionId().equals(sessionId)) {
                throw new RuntimeException("无效的会话ID");
            }

            // 5. 清空会话历史
            chatMessageRepository.deleteBySessionId(sessionId);
            
            return ResponseEntity.ok(Collections.singletonMap("message", "会话历史已清空"));
        } catch (Exception e) {
            log.error("Error clearing session history", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
