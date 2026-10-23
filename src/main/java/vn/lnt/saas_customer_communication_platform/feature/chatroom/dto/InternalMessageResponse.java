package vn.lnt.saas_customer_communication_platform.feature.chatroom.dto;

import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.InternalMessage;

import java.time.LocalDateTime;

public class InternalMessageResponse {

    private Long id;
    private Long senderId;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;

    public InternalMessageResponse() {
    }

    public InternalMessageResponse(Long id, Long senderId, String content, String messageType, LocalDateTime createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    public static InternalMessageResponse fromEntity(InternalMessage message) {
        return new InternalMessageResponse(
                message.getId(),
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
