package com.airepair.service;

import com.airepair.dto.UserDTO;
import com.airepair.entity.Team;
import com.airepair.entity.User;
import com.airepair.repository.TeamRepository;
import com.airepair.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamService {
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private UserRepository userRepository;

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Transactional
    public Team createTeam(String teamName) {
        Team team = new Team();
        team.setName(teamName);
        team.setSessionId(UUID.randomUUID().toString());
        return teamRepository.save(team);
    }

    @Transactional
    public Team createTeam(String username, String teamName) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Team team = createTeam(teamName);
        team.setLeaderId(user.getId());
        return teamRepository.save(team);
    }

    @Transactional
    public void addUserToTeam(String username, String teamName) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Team team = teamRepository.findByName(teamName)
            .orElseThrow(() -> new RuntimeException("团队不存在"));
        
        user.setTeamId(team.getId());
        userRepository.save(user);
    }

    public UserDTO getUserTeam(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        
        if (user.getTeamId() != null) {
            Team team = teamRepository.findById(user.getTeamId())
                .orElseThrow(() -> new RuntimeException("团队不存在"));
            
            boolean isTeamLeader = team.getLeaderId() != null && team.getLeaderId().equals(user.getId());
            dto.setIsTeamLeader(isTeamLeader);
            
            UserDTO.TeamDTO teamDTO = new UserDTO.TeamDTO();
            teamDTO.setId(team.getId());
            teamDTO.setName(team.getName());
            teamDTO.setSessionId(team.getSessionId());
            dto.setTeam(teamDTO);
        }
        
        return dto;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public List<UserDTO> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("团队不存在"));
        
        List<User> members = userRepository.findByTeamId(teamId);
        
        return members.stream()
            .map(user -> {
                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setRole(user.getRole());
                
                boolean isTeamLeader = team.getLeaderId() != null && team.getLeaderId().equals(user.getId());
                dto.setIsTeamLeader(isTeamLeader);
                
                UserDTO.TeamDTO teamDTO = new UserDTO.TeamDTO();
                teamDTO.setId(team.getId());
                teamDTO.setName(team.getName());
                teamDTO.setSessionId(team.getSessionId());
                dto.setTeam(teamDTO);
                
                return dto;
            })
            .collect(Collectors.toList());
    }
}
