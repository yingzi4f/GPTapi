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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Transactional
    public Team createTeam(String teamName) {
        if (teamRepository.findByName(teamName).isPresent()) {
            throw new RuntimeException("\u56e2\u961f\u540d\u5df2\u5b58\u5728");
        }

        Team team = new Team();
        team.setName(teamName);
        team.setSessionId(UUID.randomUUID().toString());
        return teamRepository.save(team);
    }

    @Transactional
    public void assignUserToTeam(String username, String teamName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("\u7528\u6237\u4e0d\u5b58\u5728: " + username));

        Team team = teamRepository.findByName(teamName)
                .orElseThrow(() -> new RuntimeException("\u56e2\u961f\u4e0d\u5b58\u5728: " + teamName));

        user.setTeamId(team.getId());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getTeamMembers(Long teamId) {
        // \u9a8c\u8bc1\u56e2\u961f\u662f\u5426\u5b58\u5728
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("\u56e2\u961f\u4e0d\u5b58\u5728: " + teamId));

        // \u83b7\u53d6\u8be5\u56e2\u961f\u7684\u6240\u6709\u6210\u5458
        List<User> teamMembers = userRepository.findByTeamId(teamId);
        
        // \u8f6c\u6362\u4e3aDTO
        return teamMembers.stream()
            .map(user -> {
                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setRole(user.getRole());
                
                // \u68c0\u67e5\u7528\u6237\u662f\u5426\u662f\u56e2\u961f\u7ec4\u957f
                boolean isTeamLeader = team.getLeaderId() != null && 
                    team.getLeaderId().equals(user.getId());
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
