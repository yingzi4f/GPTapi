package com.airepair.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    
    @JsonProperty("context_length")
    private Integer contextLength;
    
    private Boolean stream = true;
    
    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;  // system, user, assistant
        private String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
