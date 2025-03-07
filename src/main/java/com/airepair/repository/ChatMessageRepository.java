package com.airepair.repository;

import com.airepair.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId = ?1")
    void deleteBySessionId(String sessionId);
    
    long countBySessionId(String sessionId);
}
