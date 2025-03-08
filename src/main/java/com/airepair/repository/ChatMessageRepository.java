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
    
    @Query("SELECT DISTINCT c.sessionId FROM ChatMessage c WHERE c.userId = ?1 GROUP BY c.sessionId ORDER BY MAX(c.createdAt) DESC")
    List<String> findSessionIdsByUserId(Long userId);
    
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = ?1 AND c.userId = ?2 ORDER BY c.createdAt ASC")
    List<ChatMessage> findBySessionIdAndUserId(String sessionId, Long userId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId = ?1")
    void deleteBySessionId(String sessionId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId = ?1 AND c.userId = ?2")
    void deleteBySessionIdAndUserId(String sessionId, Long userId);
    
    long countBySessionId(String sessionId);
    
    long countBySessionIdAndUserId(String sessionId, Long userId);
    
    @Query("SELECT c FROM ChatMessage c WHERE c.userId = ?1 AND c.sessionId IN (SELECT DISTINCT cm.sessionId FROM ChatMessage cm WHERE cm.userId = ?1) AND c.createdAt = (SELECT MAX(cm2.createdAt) FROM ChatMessage cm2 WHERE cm2.sessionId = c.sessionId)")
    List<ChatMessage> findLatestMessagesByUserId(Long userId);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = ?1 ORDER BY m.createdAt ASC")
    List<ChatMessage> findBySessionIdOrderByCreatedAt(String sessionId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId = ?1")
    void deleteSessionHistory(String sessionId);
    
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId IN (SELECT t.sessionId FROM Team t) ORDER BY c.createdAt DESC")
    List<ChatMessage> findAllTeamMessages();
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId IN (SELECT t.sessionId FROM Team t)")
    void deleteAllTeamMessages();
}
