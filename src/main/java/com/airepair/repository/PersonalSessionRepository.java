package com.airepair.repository;

import com.airepair.entity.PersonalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalSessionRepository extends JpaRepository<PersonalSession, Long> {
    List<PersonalSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    Optional<PersonalSession> findBySessionId(String sessionId);
    
    Optional<PersonalSession> findBySessionIdAndUserId(String sessionId, Long userId);
    
    @Modifying
    @Transactional
    void deleteBySessionIdAndUserId(String sessionId, Long userId);
}
