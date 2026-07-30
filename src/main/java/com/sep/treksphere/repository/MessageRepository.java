package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
