package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import jakarta.validation.constraints.NotNull;

public class CreateConversationRequest {

    @NotNull(message = "ID kênh kết nối (channelId) không được để trống")
    private Long channelId;

    @NotNull(message = "ID khách truy cập (visitorId) không được để trống")
    private Long visitorId;

    public CreateConversationRequest() {
    }

    public CreateConversationRequest(Long channelId, Long visitorId) {
        this.channelId = channelId;
        this.visitorId = visitorId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }
}

