package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public class SendConversationMessageRequest {

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    private String messageType;

    public SendConversationMessageRequest() {
    }

    public SendConversationMessageRequest(String content, String messageType) {
        this.content = content;
        this.messageType = messageType;
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

