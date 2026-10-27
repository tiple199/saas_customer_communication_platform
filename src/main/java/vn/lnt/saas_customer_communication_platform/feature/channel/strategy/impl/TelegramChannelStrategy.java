package vn.lnt.saas_customer_communication_platform.feature.channel.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
        channel.setConfig(toJson(config));
    }

    @Override
    public void updateChannel(Channel channel, UpdateChannelRequest request) {
        if (request.getConfig() != null) {
            log.info("Cập nhật cấu hình kênh TELEGRAM Bot: {}", channel.getId());
            TelegramChannelConfig config = convertConfig(request.getConfig());
            channel.setConfig(toJson(config));
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
