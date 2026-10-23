package vn.lnt.saas_customer_communication_platform.feature.chatroom.dto;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    private String messageType = "TEXT";

    public SendMessageRequest() {
    }

    public SendMessageRequest(String content) {
        this.content = content;
        this.messageType = "TEXT";
    }

    public SendMessageRequest(String content, String messageType) {
        this.content = content;
        this.messageType = messageType != null && !messageType.trim().isEmpty() ? messageType : "TEXT";
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
}
