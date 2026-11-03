package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;

import java.time.LocalDateTime;

public class ConversationMessageResponse {

    private Long id;
    private Long conversationId;
    private String senderType;
    private Long senderId;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;

    public ConversationMessageResponse() {
    }

    public ConversationMessageResponse(Long id, Long conversationId, String senderType, Long senderId,
                                       String content, String messageType, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderType = senderType;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    public static ConversationMessageResponse fromEntity(ConversationMessage message) {
        if (message == null) {
            return null;
        }
        return new ConversationMessageResponse(
                message.getId(),
                message.getConversation() != null ? message.getConversation().getId() : null,
                message.getSenderType(),
                message.getSenderId(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

