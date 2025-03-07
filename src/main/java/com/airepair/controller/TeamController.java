package com.airepair.controller;

import com.airepair.dto.TeamRequest;
import com.airepair.dto.UserDTO;
import com.airepair.entity.Role;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.UserRepository;
import com.airepair.service.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@CrossOrigin
public class TeamController {

    @Autowired
    private TeamService teamService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        try {
            log.info("Getting all teams");
            List<Team> teams = teamService.getAllTeams();
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            log.error("Failed to get teams", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long teamId) {
        try {
            List<UserDTO> members = teamService.getTeamMembers(teamId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            log.error("Failed to get team members", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createTeam(@RequestBody TeamRequest request) {
        try {
            log.info("Creating team with name: {} by user: {}", request.getTeamName(), request.getUsername());
            
            // 检查请求参数
            if (request.getUsername() == null || request.getTeamName() == null) {
                return ResponseEntity.badRequest().body("用户名和团队名不能为空");
            }
            
            // 检查是否是管理员
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + request.getUsername()));
            
            log.info("Found user: {}, role: {}", user.getUsername(), user.getRole());
            
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.badRequest().body("只有管理员可以创建团队");
            }

            Team team = teamService.createTeam(request.getTeamName());
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            log.error("创建团队失败", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/assign")
    public ResponseEntity<?> assignUserToTeam(@RequestBody TeamRequest request) {
        try {
            log.info("Assigning user {} to team {} by admin: {}", request.getTargetUsername(), request.getTeamName(), request.getUsername());
            
            // 检查请求参数
            if (request.getUsername() == null || request.getTeamName() == null || request.getTargetUsername() == null) {
                return ResponseEntity.badRequest().body("管理员用户名、目标用户名和团队名不能为空");
            }
            
            // 检查是否是管理员
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + request.getUsername()));
            
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.badRequest().body("只有管理员可以分配用户到团队");
            }

            teamService.assignUserToTeam(request.getTargetUsername(), request.getTeamName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("分配用户到团队失败", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
