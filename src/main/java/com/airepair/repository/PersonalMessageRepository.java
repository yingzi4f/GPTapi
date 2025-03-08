package com.airepair.repository;

import com.airepair.entity.PersonalMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PersonalMessageRepository extends JpaRepository<PersonalMessage, Long> {
    List<PersonalMessage> findBySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, Long userId);
    
    @Modifying
    @Transactional
    void deleteBySessionIdAndUserId(String sessionId, Long userId);
    
    long countBySessionIdAndUserId(String sessionId, Long userId);
}
