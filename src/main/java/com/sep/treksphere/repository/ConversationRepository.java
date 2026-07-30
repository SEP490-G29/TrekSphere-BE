package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT c
            FROM Conversation c
            JOIN c.participants p
            WHERE p.userId = :userId
              AND c.isDeleted = false
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    Page<Conversation> findActiveConversationsByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
