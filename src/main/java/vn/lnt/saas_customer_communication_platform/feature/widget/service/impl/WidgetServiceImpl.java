package vn.lnt.saas_customer_communication_platform.feature.widget.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.config.WebChannelConfig;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.util.DomainValidator;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.service.WidgetService;

@Service
public class WidgetServiceImpl implements WidgetService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChannelRepository channelRepository;

    public WidgetServiceImpl(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicWidgetConfigResponse getWidgetConfig(String channelKey, String requestOrigin) {
        if (channelKey == null || channelKey.isBlank()) {
            throw new InvalidOperationException("Khóa kênh (channelKey) không được để trống");
        }

        Channel channel = channelRepository.findByChannelKey(channelKey)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "channelKey", channelKey));

        if (channel.getType() != ChannelType.WEB) {
            throw new InvalidOperationException("Kênh này không phải là kênh Website Chat Widget");
        }

        if (channel.getStatus() != ChannelStatus.ACTIVE) {
            throw new InvalidOperationException("Kênh kết nối hiện không hoạt động");
        }

        WebChannelConfig webConfig = null;
        if (channel.getConfig() != null && !channel.getConfig().isBlank()) {
            try {
                webConfig = OBJECT_MAPPER.readValue(channel.getConfig(), WebChannelConfig.class);
            } catch (Exception e) {
                // Giữ webConfig là null hoặc mặc định
            }
        }

        String allowedDomains = webConfig != null ? webConfig.getAllowedDomains() : null;
        if (!DomainValidator.isOriginAllowed(requestOrigin, allowedDomains)) {
            throw new InvalidOperationException("Tên miền của bạn không được phép sử dụng Widget này");
        }

        String welcomeMsg = webConfig != null ? webConfig.getWelcomeMessage() : null;
        return new PublicWidgetConfigResponse(
                channel.getChannelKey(),
                channel.getName(),
                welcomeMsg,
                channel.getAiEnabled() != null ? channel.getAiEnabled() : false
        );
    }
}
