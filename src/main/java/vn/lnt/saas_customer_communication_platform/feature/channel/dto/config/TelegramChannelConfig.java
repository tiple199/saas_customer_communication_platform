package vn.lnt.saas_customer_communication_platform.feature.channel.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO đại diện cho cấu hình của kênh Telegram Bot.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramChannelConfig {

    private String botToken;
    private String botUsername;

    public TelegramChannelConfig() {
    }

    public TelegramChannelConfig(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }
}
