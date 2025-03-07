package com.airepair.service;

import com.airepair.dto.UserDTO;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.TeamRepository;
import com.airepair.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> {
                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setRole(user.getRole());
                
                if (user.getTeamId() != null) {
                    Team team = teamRepository.findById(user.getTeamId()).orElse(null);
                    if (team != null) {
                        // 检查用户是否是团队组长
                        boolean isTeamLeader = team.getLeaderId() != null && 
                            team.getLeaderId().equals(user.getId());
                        dto.setIsTeamLeader(isTeamLeader);
                        
                        UserDTO.TeamDTO teamDTO = new UserDTO.TeamDTO();
                        teamDTO.setId(team.getId());
                        teamDTO.setName(team.getName());
                        teamDTO.setSessionId(team.getSessionId());
                        dto.setTeam(teamDTO);
                    }
                } else {
                    dto.setIsTeamLeader(false);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public void removeUserFromTeam(String username, Long teamId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
            
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("团队不存在: " + teamId));
            
        // 检查用户是否在指定的团队中
        if (user.getTeamId() == null || !user.getTeamId().equals(teamId)) {
            throw new RuntimeException("用户 " + username + " 不在团队 " + team.getName() + " 中");
        }
        
        // 如果用户是组长，需要先取消组长身份
        if (team.getLeaderId() != null && team.getLeaderId().equals(user.getId())) {
            team.setLeaderId(null);
            teamRepository.save(team);
        }
        
        user.setTeamId(null);
        userRepository.save(user);
    }
}
