package com.airepair.dto;

import com.airepair.entity.Role;
import lombok.Data;

@Data
public class LoginResponse {
    private Long id;
    private String username;
    private Role role;
    private TeamInfo team;
    
    @Data
    public static class TeamInfo {
        private Long id;
        private String name;
        private String sessionId;
    }
}
