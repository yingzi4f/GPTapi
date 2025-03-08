package com.airepair.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSessionDTO {
    private String sessionId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int messageCount;
    private String modelName;
}
