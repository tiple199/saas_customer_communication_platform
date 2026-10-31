package vn.lnt.saas_customer_communication_platform.feature.widget.dto;

import java.time.LocalDateTime;

public class WidgetHandshakeResponse {

    private String channelKey;
    private String channelName;
    private String welcomeMessage;
    private Boolean aiEnabled;
    private Long visitorId;
    private String visitorToken;
    private String visitorName;
    private String visitorEmail;
    private Boolean isNewVisitor;
    private LocalDateTime connectedAt;

    public WidgetHandshakeResponse() {
    }

    public WidgetHandshakeResponse(String channelKey, String channelName, String welcomeMessage, Boolean aiEnabled,
                                   Long visitorId, String visitorToken, String visitorName, String visitorEmail,
                                   Boolean isNewVisitor, LocalDateTime connectedAt) {
        this.channelKey = channelKey;
        this.channelName = channelName;
        this.welcomeMessage = welcomeMessage;
        this.aiEnabled = aiEnabled;
        this.visitorId = visitorId;
        this.visitorToken = visitorToken;
        this.visitorName = visitorName;
        this.visitorEmail = visitorEmail;
        this.isNewVisitor = isNewVisitor;
        this.connectedAt = connectedAt;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public Boolean getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public String getVisitorToken() {
        return visitorToken;
    }

    public void setVisitorToken(String visitorToken) {
        this.visitorToken = visitorToken;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public String getVisitorEmail() {
        return visitorEmail;
    }

    public void setVisitorEmail(String visitorEmail) {
        this.visitorEmail = visitorEmail;
    }

    public Boolean getIsNewVisitor() {
        return isNewVisitor;
    }

    public void setIsNewVisitor(Boolean isNewVisitor) {
        this.isNewVisitor = isNewVisitor;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
}
