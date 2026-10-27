package vn.lnt.saas_customer_communication_platform.feature.channel.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO đại diện cho cấu hình của kênh Website Livechat Widget.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebChannelConfig {

    private String widgetKey;

    private String welcomeMessage;

    private String allowedDomains;

    public WebChannelConfig() {
    }

    public WebChannelConfig(String widgetKey, String welcomeMessage, String allowedDomains) {
        this.widgetKey = widgetKey;
        this.welcomeMessage = welcomeMessage;
        this.allowedDomains = allowedDomains;
    }

    public String getWidgetKey() {
        return widgetKey;
    }

    public void setWidgetKey(String widgetKey) {
        this.widgetKey = widgetKey;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(String allowedDomains) {
        this.allowedDomains = allowedDomains;
    }
}
