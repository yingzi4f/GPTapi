package com.airepair.controller;

import com.airepair.dto.UserDTO;
import com.airepair.dto.SetTeamLeaderRequest;
import com.airepair.entity.Role;
import com.airepair.entity.User;
import com.airepair.entity.Team;
import com.airepair.repository.UserRepository;
import com.airepair.repository.TeamRepository;
import com.airepair.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        try {
            log.info("Getting all users with team info");
            List<UserDTO> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Failed to get users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{username}/teams/{teamId}")
    public ResponseEntity<?> removeUserFromTeam(
            @PathVariable String username,
            @PathVariable Long teamId,
            @RequestBody Map<String, String> request) {
        try {
            // 检查操作者是否是管理员
            String adminUsername = request.get("username");
            if (adminUsername == null) {
                return ResponseEntity.badRequest().body("需要提供管理员用户名");
            }

            User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("管理员用户不存在"));

            if (admin.getRole() != Role.ADMIN) {
                return ResponseEntity.badRequest().body("只有管理员可以移除用户的团队");
            }

            userService.removeUserFromTeam(username, teamId);
            return ResponseEntity.ok().body("用户已从团队中移除");
        } catch (Exception e) {
            log.error("Failed to remove user from team", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/set-team-leader")
    public ResponseEntity<?> setTeamLeader(@RequestBody SetTeamLeaderRequest request) {
        try {
            log.info("Setting user {} as team leader for team {}", request.getUsername(), request.getTeamId());
            
            // 获取用户
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + request.getUsername()));

            // 获取团队
            Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("团队不存在: " + request.getTeamId()));

            // 如果团队已有组长，先移除原组长的角色
            if (team.getLeaderId() != null) {
                User oldLeader = userRepository.findById(team.getLeaderId())
                    .orElse(null);
                if (oldLeader != null) {
                    oldLeader.setRole(Role.USER);
                    userRepository.save(oldLeader);
                    log.info("Removed team leader role from user: {}", oldLeader.getUsername());
                }
            }

            // 设置新组长
            user.setRole(Role.TEAM_LEADER);
            team.setLeaderId(user.getId());
            
            // 保存更改
            userRepository.save(user);
            teamRepository.save(team);
            
            log.info("Successfully set user {} as team leader for team {}", user.getUsername(), team.getId());
            return ResponseEntity.ok()
                .body(Map.of(
                    "message", "Successfully set team leader",
                    "username", user.getUsername(),
                    "teamId", team.getId()
                ));
                
        } catch (Exception e) {
            log.error("Failed to set team leader", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
