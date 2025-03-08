package com.airepair.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "username")
    private String username;

    @Column(name = "model_name")
    private String modelName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
