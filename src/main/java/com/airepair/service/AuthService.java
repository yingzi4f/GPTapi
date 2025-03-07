package com.airepair.service;

import com.airepair.dto.LoginRequest;
import com.airepair.dto.LoginResponse;
import com.airepair.dto.RegisterRequest;
import com.airepair.entity.Role;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.TeamRepository;
import com.airepair.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TeamRepository teamRepository;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!request.getPassword().equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());

        // 如果用户属于某个团队，获取团队信息
        if (user.getTeamId() != null) {
            Team team = teamRepository.findById(user.getTeamId())
                .orElse(null);
            if (team != null) {
                LoginResponse.TeamInfo teamInfo = new LoginResponse.TeamInfo();
                teamInfo.setId(team.getId());
                teamInfo.setName(team.getName());
                teamInfo.setSessionId(team.getSessionId());
                response.setTeam(teamInfo);
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public boolean isTeamLeader(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getTeamId() == null) {
            return false;
        }
        
        Team team = teamRepository.findById(user.getTeamId())
            .orElse(null);
            
        return team != null && 
               team.getLeaderId() != null && 
               team.getLeaderId().equals(user.getId());
    }

    @Transactional(readOnly = true)
    public boolean isSameTeam(String username1, String username2) {
        User user1 = userRepository.findByUsername(username1)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username1));
        User user2 = userRepository.findByUsername(username2)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username2));

        if (user1.getTeamId() == null || user2.getTeamId() == null) {
            return false;
        }

        return user1.getTeamId().equals(user2.getTeamId());
    }
}
