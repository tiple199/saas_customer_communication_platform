package vn.lnt.saas_customer_communication_platform.feature.channel.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.config.WebChannelConfig;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.channel.strategy.ChannelStrategy;

import java.util.Map;
import java.util.UUID;

@Component
public class WebChannelStrategy implements ChannelStrategy {

    private static final Logger log = LoggerFactory.getLogger(WebChannelStrategy.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEB;
    }

    @Override
    public void initChannel(Channel channel, CreateChannelRequest request) {
        log.info("Khởi tạo cấu hình kênh WEB Livechat cho: {}", channel.getName());
        WebChannelConfig config = convertConfig(request.getConfig());

        // Nếu chưa có widgetKey, tự động sinh khóa nhận diện cho widget
        if (config.getWidgetKey() == null || config.getWidgetKey().isBlank()) {
            config.setWidgetKey("web_" + UUID.randomUUID().toString().replace("-", ""));
        }

        channel.setConfig(toJson(config));
    }

    @Override
    public void updateChannel(Channel channel, UpdateChannelRequest request) {
        if (request.getConfig() != null) {
            log.info("Cập nhật cấu hình kênh WEB Livechat: {}", channel.getId());
            WebChannelConfig newConfig = convertConfig(request.getConfig());

            // Giữ lại widgetKey đã sinh trước đó nếu client không truyền
            WebChannelConfig currentConfig = parseConfigFromJson(channel.getConfig());
            if (newConfig.getWidgetKey() == null && currentConfig.getWidgetKey() != null) {
                newConfig.setWidgetKey(currentConfig.getWidgetKey());
            }

            channel.setConfig(toJson(newConfig));
        }
    }

    private WebChannelConfig convertConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new WebChannelConfig();
        }
        try {
            return objectMapper.convertValue(map, WebChannelConfig.class);
        } catch (Exception e) {
            log.warn("Lỗi chuyển đổi config map sang WebChannelConfig: {}", e.getMessage());
            return new WebChannelConfig();
        }
    }

    private WebChannelConfig parseConfigFromJson(String json) {
        if (json == null || json.isBlank()) {
            return new WebChannelConfig();
        }
        try {
            return objectMapper.readValue(json, WebChannelConfig.class);
        } catch (Exception e) {
            return new WebChannelConfig();
        }
    }

    private String toJson(WebChannelConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.error("Lỗi chuyển đổi WebChannelConfig sang JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
