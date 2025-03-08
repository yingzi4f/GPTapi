package com.airepair.controller;

import com.airepair.dto.TeamRequest;
import com.airepair.dto.UserDTO;
import com.airepair.entity.Role;
import com.airepair.entity.User;
import com.airepair.service.TeamService;
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
@RequestMapping("/api/teams")
@CrossOrigin
public class TeamController {
    
    @Autowired
    private TeamService teamService;

    @GetMapping("/list")
    public ResponseEntity<?> listTeams() {
        try {
            log.info("Getting all teams");
            return ResponseEntity.ok(teamService.getAllTeams());
        } catch (Exception e) {
            log.error("Error listing teams", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserTeam(@PathVariable String username) {
        try {
            log.info("Getting team for user: {}", username);
            return ResponseEntity.ok(teamService.getUserTeam(username));
        } catch (Exception e) {
            log.error("Error getting user team", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long teamId) {
        try {
            log.info("Getting members for team: {}", teamId);
            List<UserDTO> members = teamService.getTeamMembers(teamId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            log.error("Error getting team members", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTeam(@RequestBody TeamRequest request) {
        try {
            log.info("Creating team {} for user {}", request.getTeamName(), request.getUsername());
            
            if (request.getUsername() == null || request.getTeamName() == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "用户名和团队名不能为空"));
            }
            
            User user = teamService.getUserByUsername(request.getUsername());
            
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "只有管理员可以创建团队"));
            }
            
            return ResponseEntity.ok(teamService.createTeam(request.getUsername(), request.getTeamName()));
        } catch (Exception e) {
            log.error("Error creating team", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUserToTeam(@RequestBody TeamRequest request) {
        try {
            log.info("Adding user {} to team {} by {}", request.getTargetUsername(), request.getTeamName(), request.getUsername());
            
            if (request.getUsername() == null || request.getTeamName() == null || request.getTargetUsername() == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "管理员用户名、目标用户名和团队名不能为空"));
            }
            
            User user = teamService.getUserByUsername(request.getUsername());
            
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "只有管理员可以添加团队成员"));
            }
            
            teamService.addUserToTeam(request.getTargetUsername(), request.getTeamName());
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户已成功添加到团队");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding user to team", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
