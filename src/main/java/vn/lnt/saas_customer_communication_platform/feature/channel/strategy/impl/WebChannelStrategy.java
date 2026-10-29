package vn.lnt.saas_customer_communication_platform.feature.channel.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.config.WebChannelConfig;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.channel.strategy.ChannelStrategy;
import vn.lnt.saas_customer_communication_platform.feature.channel.util.DomainValidator;

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

        // Nếu chưa có widgetKey, tự động gán bằng channelKey hoặc sinh khóa nhận diện cho widget
        if (config.getWidgetKey() == null || config.getWidgetKey().isBlank()) {
            config.setWidgetKey(channel.getChannelKey() != null ? channel.getChannelKey() : "web_" + UUID.randomUUID().toString().replace("-", ""));
        }

        validate(config);
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

            validate(newConfig);
            channel.setConfig(toJson(newConfig));
        }
    }

    private void validate(WebChannelConfig config) {
        if (config.getWelcomeMessage() != null && config.getWelcomeMessage().length() > 500) {
            throw new InvalidOperationException("Lời chào không được vượt quá 500 ký tự");
        }
        if (config.getWidgetKey() != null && config.getWidgetKey().length() > 100) {
            throw new InvalidOperationException("Widget key không được vượt quá 100 ký tự");
        }
        if (config.getAllowedDomains() != null && !config.getAllowedDomains().isBlank()) {
            String normalizedDomains = DomainValidator.validateAndNormalize(config.getAllowedDomains());
            config.setAllowedDomains(normalizedDomains);
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
