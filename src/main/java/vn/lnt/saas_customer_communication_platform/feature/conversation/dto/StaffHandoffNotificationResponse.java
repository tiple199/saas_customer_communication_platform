package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import java.time.LocalDateTime;

public class StaffHandoffNotificationResponse {

    private String type;
    private Long companyId;
    private Long conversationId;
    private Long channelId;
    private String channelName;
    private Long visitorId;
    private String visitorName;
    private String lastCustomerMessage;
    private String reason;
    private String message;
    private LocalDateTime createdAt;

    public StaffHandoffNotificationResponse() {
    }

    public StaffHandoffNotificationResponse(String type, Long companyId, Long conversationId,
                                            Long channelId, String channelName,
                                            Long visitorId, String visitorName,
                                            String lastCustomerMessage, String reason,
                                            String message, LocalDateTime createdAt) {
        this.type = type;
        this.companyId = companyId;
        this.conversationId = conversationId;
        this.channelId = channelId;
        this.channelName = channelName;
        this.visitorId = visitorId;
        this.visitorName = visitorName;
        this.lastCustomerMessage = lastCustomerMessage;
        this.reason = reason;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public String getLastCustomerMessage() {
        return lastCustomerMessage;
    }

    public void setLastCustomerMessage(String lastCustomerMessage) {
        this.lastCustomerMessage = lastCustomerMessage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

