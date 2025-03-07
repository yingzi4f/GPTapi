package com.airepair.dto;

import lombok.Data;

@Data
public class SetTeamLeaderRequest {
    private String username;  // 要设置为组长的用户名
    private Long teamId;     // 团队ID
}
