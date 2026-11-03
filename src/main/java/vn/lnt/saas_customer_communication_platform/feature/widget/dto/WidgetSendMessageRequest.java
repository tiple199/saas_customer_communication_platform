package vn.lnt.saas_customer_communication_platform.feature.widget.dto;

import jakarta.validation.constraints.NotBlank;

public class WidgetSendMessageRequest {

    @NotBlank(message = "Khóa kênh (channelKey) không được để trống")
    private String channelKey;

    @NotBlank(message = "Mã phiên khách truy cập (visitorToken) không được để trống")
    private String visitorToken;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    private String messageType;

    public WidgetSendMessageRequest() {
    }

    public WidgetSendMessageRequest(String channelKey, String visitorToken, String content, String messageType) {
        this.channelKey = channelKey;
        this.visitorToken = visitorToken;
        this.content = content;
        this.messageType = messageType;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public String getVisitorToken() {
        return visitorToken;
    }

    public void setVisitorToken(String visitorToken) {
        this.visitorToken = visitorToken;
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

