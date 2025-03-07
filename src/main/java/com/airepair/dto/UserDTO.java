package com.airepair.dto;

import com.airepair.entity.Role;
import com.airepair.entity.Team;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private Role role;
    private TeamDTO team;
    private Boolean isTeamLeader;

    @Data
    public static class TeamDTO {
        private Long id;
        private String name;
        private String sessionId;
    }
}
