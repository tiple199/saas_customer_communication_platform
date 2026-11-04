package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import jakarta.validation.constraints.NotNull;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;

public class UpdateConversationStatusRequest {

    @NotNull(message = "Trạng thái cuộc hội thoại không được để trống")
    private ConversationStatus status;

    public UpdateConversationStatusRequest() {
    }

    public UpdateConversationStatusRequest(ConversationStatus status) {
        this.status = status;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }
}

