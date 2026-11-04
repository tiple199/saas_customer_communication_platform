package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

public class AssignConversationRequest {

    private Long assigneeId;

    public AssignConversationRequest() {
    }

    public AssignConversationRequest(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}

