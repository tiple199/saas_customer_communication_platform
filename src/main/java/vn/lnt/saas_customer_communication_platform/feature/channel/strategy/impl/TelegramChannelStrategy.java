package vn.lnt.saas_customer_communication_platform.feature.channel.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.config.TelegramChannelConfig;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.channel.strategy.ChannelStrategy;

import java.util.Map;

@Component
public class TelegramChannelStrategy implements ChannelStrategy {

    private static final Logger log = LoggerFactory.getLogger(TelegramChannelStrategy.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChannelType getChannelType() {
        return ChannelType.TELEGRAM;
    }

    @Override
    public void initChannel(Channel channel, CreateChannelRequest request) {
        log.info("Khởi tạo cấu hình kênh TELEGRAM Bot cho: {}", channel.getName());
        TelegramChannelConfig config = convertConfig(request.getConfig());
        validate(config);
        channel.setConfig(toJson(config));
    }

    @Override
    public void updateChannel(Channel channel, UpdateChannelRequest request) {
        if (request.getConfig() != null) {
            log.info("Cập nhật cấu hình kênh TELEGRAM Bot: {}", channel.getId());
            TelegramChannelConfig newConfig = convertConfig(request.getConfig());
            TelegramChannelConfig currentConfig = parseConfigFromJson(channel.getConfig());

            if (newConfig.getBotToken() == null && currentConfig.getBotToken() != null) {
                newConfig.setBotToken(currentConfig.getBotToken());
            }
            if (newConfig.getBotUsername() == null && currentConfig.getBotUsername() != null) {
                newConfig.setBotUsername(currentConfig.getBotUsername());
            }

            validate(newConfig);
            channel.setConfig(toJson(newConfig));
        }
    }

    private void validate(TelegramChannelConfig config) {
        if (config.getBotToken() == null || config.getBotToken().trim().isEmpty()) {
            throw new InvalidOperationException("Bot token của Telegram là bắt buộc và không được để trống");
        }
        if (config.getBotToken().length() > 255) {
            throw new InvalidOperationException("Bot token của Telegram không được vượt quá 255 ký tự");
        }
        if (config.getBotUsername() != null && config.getBotUsername().length() > 100) {
            throw new InvalidOperationException("Bot username của Telegram không được vượt quá 100 ký tự");
        }
    }

    private TelegramChannelConfig parseConfigFromJson(String json) {
        if (json == null || json.isBlank()) {
            return new TelegramChannelConfig();
        }
        try {
            return objectMapper.readValue(json, TelegramChannelConfig.class);
        } catch (Exception e) {
            return new TelegramChannelConfig();
        }
    }

    private TelegramChannelConfig convertConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new TelegramChannelConfig();
        }
        try {
            return objectMapper.convertValue(map, TelegramChannelConfig.class);
        } catch (Exception e) {
            log.warn("Lỗi chuyển đổi config map sang TelegramChannelConfig: {}", e.getMessage());
            return new TelegramChannelConfig();
        }
    }

    private String toJson(TelegramChannelConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.error("Lỗi chuyển đổi TelegramChannelConfig sang JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
