package com.sep.treksphere.chat.message;

import com.sep.treksphere.chat.Conversation;
import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor


public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(length = 500)
    private String attachmentStorageId;

    @Column(length = 1000)
    private String attachmentUrl;

    @Column(length = 255)
    private String attachmentName;

    @Column(length = 255)
    private String attachmentMimeType;

    private Long attachmentSizeBytes;

    @Column(nullable = false)
    private Boolean isRead = false;
}
