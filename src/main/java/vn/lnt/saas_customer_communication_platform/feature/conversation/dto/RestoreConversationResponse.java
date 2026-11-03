package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import java.util.List;

public class RestoreConversationResponse {

    private ConversationResponse conversation;
    private List<ConversationMessageResponse> messages;

    public RestoreConversationResponse() {
    }

    public RestoreConversationResponse(ConversationResponse conversation, List<ConversationMessageResponse> messages) {
        this.conversation = conversation;
        this.messages = messages;
    }

    public ConversationResponse getConversation() {
        return conversation;
    }

    public void setConversation(ConversationResponse conversation) {
        this.conversation = conversation;
    }

    public List<ConversationMessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<ConversationMessageResponse> messages) {
        this.messages = messages;
    }
}

