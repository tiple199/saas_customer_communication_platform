package vn.lnt.saas_customer_communication_platform.feature.widget.dto;

public class PublicWidgetConfigResponse {

    private String channelKey;
    private String name;
    private String welcomeMessage;
    private Boolean aiEnabled;

    public PublicWidgetConfigResponse() {
    }

    public PublicWidgetConfigResponse(String channelKey, String name, String welcomeMessage, Boolean aiEnabled) {
        this.channelKey = channelKey;
        this.name = name;
        this.welcomeMessage = welcomeMessage;
        this.aiEnabled = aiEnabled;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
