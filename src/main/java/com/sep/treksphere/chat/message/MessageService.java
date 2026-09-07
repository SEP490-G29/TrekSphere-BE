package com.sep.treksphere.chat.message;

import com.sep.treksphere.chat.Conversation;
import com.sep.treksphere.chat.ConversationRepository;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.file.StoredFile;
import com.sep.treksphere.file.UploadPolicy;
import com.sep.treksphere.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final FileService fileService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendText(MessageCreateRequest request, CustomUserDetails userDetails) {
        Conversation conversation = requireParticipant(request.getConversationId(), userDetails);
        Message message = baseMessage(conversation, userDetails.getUser());
        message.setMessageType(MessageType.TEXT);
        message.setContent(request.getContent().trim());
        return persistAndBroadcast(message, conversation);
    }

    @Transactional
    public MessageResponse sendFile(
            UUID conversationId,
            String caption,
            MultipartFile file,
            CustomUserDetails userDetails) {
        Conversation conversation = requireParticipant(conversationId, userDetails);
        StoredFile storedFile = fileService.upload(
                file, "chat/" + conversationId, UploadPolicy.CHAT_ATTACHMENT);
        try {
            Message message = baseMessage(conversation, userDetails.getUser());
            message.setMessageType(storedFile.mimeType().startsWith("image/")
                    ? MessageType.IMAGE : MessageType.FILE);
            message.setContent(StringUtils.hasText(caption) ? caption.trim() : null);
            message.setAttachmentStorageId(storedFile.storageId());
            message.setAttachmentUrl(storedFile.url());
            message.setAttachmentName(storedFile.originalName());
            message.setAttachmentMimeType(storedFile.mimeType());
            message.setAttachmentSizeBytes(storedFile.sizeBytes());
            return persistAndBroadcast(message, conversation);
        } catch (RuntimeException ex) {
            fileService.deleteFileBestEffort(storedFile.storageId(), storedFile.mimeType());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public String getAttachmentUrl(UUID messageId, CustomUserDetails userDetails) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_ATTACHMENT_NOT_FOUND));
        requireParticipant(message.getConversation().getConversationId(), userDetails);
        if ((message.getMessageType() != MessageType.IMAGE && message.getMessageType() != MessageType.FILE)
                || !StringUtils.hasText(message.getAttachmentUrl())) {
            throw new AppException(ErrorCode.MESSAGE_ATTACHMENT_NOT_FOUND);
        }
        return message.getAttachmentUrl();
    }

    public MessageResponse toResponse(Message message) {
        User sender = message.getSender();
        AttachmentResponse attachment = null;
        if (message.getMessageType() == MessageType.IMAGE || message.getMessageType() == MessageType.FILE) {
            attachment = AttachmentResponse.builder()
                    .name(message.getAttachmentName())
                    .mimeType(message.getAttachmentMimeType())
                    .sizeBytes(message.getAttachmentSizeBytes())
                    .downloadUrl("/api/v1/chat/messages/" + message.getMessageId() + "/attachment")
                    .build();
        }
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation().getConversationId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .attachment(attachment)
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private Conversation requireParticipant(UUID conversationId, CustomUserDetails userDetails) {
        return conversationRepository.findActiveConversationByIdAndParticipantId(
                        conversationId, userDetails.getUser().getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private Message baseMessage(Conversation conversation, User sender) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setIsRead(false);
        return message;
    }

    private MessageResponse persistAndBroadcast(Message message, Conversation conversation) {
        Message saved = messageRepository.saveAndFlush(message);
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);
        MessageResponse response = toResponse(saved);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(
                        "/topic/chat/conversations/" + response.getConversationId() + "/messages",
                        response);
            }
        });
        return response;
    }
}
