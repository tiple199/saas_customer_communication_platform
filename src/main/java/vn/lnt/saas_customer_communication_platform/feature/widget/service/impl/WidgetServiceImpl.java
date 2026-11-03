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
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.VisitorResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.ConversationService;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.VisitorService;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetSendMessageRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.service.WidgetService;

import java.time.LocalDateTime;

@Service
public class WidgetServiceImpl implements WidgetService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChannelRepository channelRepository;
    private final VisitorService visitorService;
    private final ConversationService conversationService;

    public WidgetServiceImpl(ChannelRepository channelRepository,
                             VisitorService visitorService,
                             ConversationService conversationService) {
        this.channelRepository = channelRepository;
        this.visitorService = visitorService;
        this.conversationService = conversationService;
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

        // Khởi tạo hoặc khôi phục phiên Visitor thông qua VisitorService của feature.conversation
        VisitorResolutionResult visitorResult = visitorService.resolveOrCreateVisitor(
                channel.getCompany(),
                request.getVisitorToken(),
                request.getName(),
                request.getEmail()
        );
        Visitor visitor = visitorResult.getVisitor();
        boolean isNewVisitor = visitorResult.isNewVisitor();

        // Khởi tạo hoặc khôi phục Conversation đang hoạt động cho Visitor trên Channel này
        ConversationResolutionResult conversationResult = conversationService.getOrCreateActiveConversation(channel, visitor);
        Conversation conversation = conversationResult.getConversation();

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
                conversation.getId(),
                conversation.getStatus(),
                conversation.getHandlingMode(),
                conversationResult.isNewConversation(),
                now
        );
    }

    @Override
    @Transactional
    public RestoreConversationResponse restoreConversation(String channelKey, String visitorToken, String requestOrigin) {
        Channel channel = validateAndGetWebChannel(channelKey);
        WebChannelConfig webConfig = parseWebConfig(channel);
        validateOrigin(requestOrigin, webConfig);

        Visitor visitor = visitorService.getVisitorByToken(channel.getCompany().getId(), visitorToken);
        return conversationService.restoreVisitorConversation(channel, visitor);
    }

    @Override
    @Transactional
    public ConversationMessageResponse sendMessage(WidgetSendMessageRequest request, String requestOrigin) {
        if (request == null) {
            throw new InvalidOperationException("Dữ liệu tin nhắn không được để trống");
        }

        Channel channel = validateAndGetWebChannel(request.getChannelKey());
        WebChannelConfig webConfig = parseWebConfig(channel);
        validateOrigin(requestOrigin, webConfig);

        channel.setLastSeenAt(LocalDateTime.now());
        channelRepository.save(channel);

        Visitor visitor = visitorService.getVisitorByToken(channel.getCompany().getId(), request.getVisitorToken());
        return conversationService.sendVisitorMessage(
                channel,
                visitor,
                request.getContent(),
                request.getMessageType()
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
