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
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.VisitorRepository;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.service.WidgetService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WidgetServiceImpl implements WidgetService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_VISITOR_NAME = "Khách truy cập";

    private final ChannelRepository channelRepository;
    private final VisitorRepository visitorRepository;

    public WidgetServiceImpl(ChannelRepository channelRepository, VisitorRepository visitorRepository) {
        this.channelRepository = channelRepository;
        this.visitorRepository = visitorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicWidgetConfigResponse getWidgetConfig(String channelKey, String requestOrigin) {
        Channel channel = validateAndGetWebChannel(channelKey);
        WebChannelConfig webConfig = parseWebConfig(channel);
        validateOrigin(requestOrigin, webConfig);

        String welcomeMsg = webConfig != null ? webConfig.getWelcomeMessage() : null;
        return new PublicWidgetConfigResponse(
                channel.getChannelKey(),
                channel.getName(),
                welcomeMsg,
                channel.getAiEnabled() != null ? channel.getAiEnabled() : false
        );
    }

    @Override
    @Transactional
    public WidgetHandshakeResponse handshake(WidgetHandshakeRequest request, String requestOrigin) {
        if (request == null) {
            throw new InvalidOperationException("Dữ liệu khởi tạo kết nối Widget không được để trống");
        }

        Channel channel = validateAndGetWebChannel(request.getChannelKey());
        WebChannelConfig webConfig = parseWebConfig(channel);
        validateOrigin(requestOrigin, webConfig);

        LocalDateTime now = LocalDateTime.now();

        // Cập nhật thời điểm kết nối lần đầu và lần hoạt động gần nhất của Channel
        if (channel.getFirstConnectedAt() == null) {
            channel.setFirstConnectedAt(now);
        }
        channel.setLastSeenAt(now);
        channelRepository.save(channel);

        // Khởi tạo hoặc khôi phục phiên Visitor
        Visitor visitor = null;
        boolean isNewVisitor = true;

        if (request.getVisitorToken() != null && !request.getVisitorToken().isBlank() && channel.getCompany() != null) {
            Optional<Visitor> existingVisitor = visitorRepository.findByVisitorTokenAndCompanyId(
                    request.getVisitorToken().trim(),
                    channel.getCompany().getId()
            );
            if (existingVisitor.isPresent()) {
                visitor = existingVisitor.get();
                isNewVisitor = false;
                visitor.setLastSeenAt(now);
                if (request.getName() != null && !request.getName().isBlank()) {
                    visitor.setName(request.getName().trim());
                }
                if (request.getEmail() != null && !request.getEmail().isBlank()) {
                    visitor.setEmail(request.getEmail().trim());
                }
                visitor = visitorRepository.save(visitor);
            }
        }

        if (visitor == null) {
            visitor = new Visitor();
            visitor.setVisitorToken(Visitor.generateVisitorToken());
            visitor.setCompany(channel.getCompany());
            visitor.setLastSeenAt(now);
            visitor.setName(request.getName() != null && !request.getName().isBlank()
                    ? request.getName().trim()
                    : DEFAULT_VISITOR_NAME);
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                visitor.setEmail(request.getEmail().trim());
            }
            visitor = visitorRepository.save(visitor);
        }

        String welcomeMsg = webConfig != null ? webConfig.getWelcomeMessage() : null;
        return new WidgetHandshakeResponse(
                channel.getChannelKey(),
                channel.getName(),
                welcomeMsg,
                channel.getAiEnabled() != null ? channel.getAiEnabled() : false,
                visitor.getId(),
                visitor.getVisitorToken(),
                visitor.getName(),
                visitor.getEmail(),
                isNewVisitor,
                now
        );
    }

    private Channel validateAndGetWebChannel(String channelKey) {
        if (channelKey == null || channelKey.isBlank()) {
            throw new InvalidOperationException("Khóa kênh (channelKey) không được để trống");
        }

        Channel channel = channelRepository.findByChannelKey(channelKey.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "channelKey", channelKey));

        if (channel.getType() != ChannelType.WEB) {
            throw new InvalidOperationException("Kênh này không phải là kênh Website Chat Widget");
        }

        if (channel.getStatus() != ChannelStatus.ACTIVE) {
            throw new InvalidOperationException("Kênh kết nối hiện không hoạt động");
        }

        return channel;
    }

    private WebChannelConfig parseWebConfig(Channel channel) {
        if (channel.getConfig() != null && !channel.getConfig().isBlank()) {
            try {
                return OBJECT_MAPPER.readValue(channel.getConfig(), WebChannelConfig.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void validateOrigin(String requestOrigin, WebChannelConfig webConfig) {
        String allowedDomains = webConfig != null ? webConfig.getAllowedDomains() : null;
        if (!DomainValidator.isOriginAllowed(requestOrigin, allowedDomains)) {
            throw new InvalidOperationException("Tên miền của bạn không được phép sử dụng Widget này");
        }
    }
}
