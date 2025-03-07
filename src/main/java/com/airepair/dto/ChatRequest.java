package com.airepair.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;
    private String modelName;
    private String content;
    private String username;
}
