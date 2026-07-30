package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findFirstByConversationConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID conversationId
    );

    long countByConversationConversationIdAndSenderUserIdNotAndIsReadFalseAndIsDeletedFalse(
            UUID conversationId,
            UUID userId
    );

    Page<Message> findByConversationConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID conversationId,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE Message m
            SET m.isRead = true
            WHERE m.conversation.conversationId = :conversationId
              AND m.sender.userId <> :userId
              AND m.isRead = false
              AND m.isDeleted = false
            """)
    int markMessagesAsRead(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId
    );
}
