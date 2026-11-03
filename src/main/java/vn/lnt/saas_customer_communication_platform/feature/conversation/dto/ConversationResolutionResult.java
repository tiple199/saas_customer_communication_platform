package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;

public class ConversationResolutionResult {

    private final Conversation conversation;
    private final boolean newConversation;

    public ConversationResolutionResult(Conversation conversation, boolean newConversation) {
        this.conversation = conversation;
        this.newConversation = newConversation;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public boolean isNewConversation() {
        return newConversation;
    }
}

