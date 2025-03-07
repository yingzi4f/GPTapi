package com.airepair.dto;

import lombok.Data;

@Data
public class TeamRequest {
    private String teamName;
    private String username;
    private String targetUsername;  // 要添加到团队的用户名
}
