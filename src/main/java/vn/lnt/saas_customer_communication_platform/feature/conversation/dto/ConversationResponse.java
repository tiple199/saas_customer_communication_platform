package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.HandlingMode;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;

import java.time.LocalDateTime;

public class ConversationResponse {

    private Long id;
    private ConversationStatus status;
    private HandlingMode handlingMode;
    private Long assigneeId;
    private Long visitorId;
    private String visitorToken;
    private String visitorName;
    private String visitorEmail;
    private Long channelId;
    private String channelName;
    private ChannelType channelType;
    private Long companyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public ConversationResponse() {
    }

    public ConversationResponse(Long id, ConversationStatus status, HandlingMode handlingMode, Long assigneeId,
                                Long visitorId, String visitorToken, String visitorName, String visitorEmail,
                                Long channelId, String channelName, ChannelType channelType, Long companyId,
                                LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.status = status;
        this.handlingMode = handlingMode;
        this.assigneeId = assigneeId;
        this.visitorId = visitorId;
        this.visitorToken = visitorToken;
        this.visitorName = visitorName;
        this.visitorEmail = visitorEmail;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelType = channelType;
        this.companyId = companyId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    public static ConversationResponse fromEntity(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        Visitor visitor = conversation.getVisitor();
        Channel channel = conversation.getChannel();
        Long companyId = null;
        if (channel != null && channel.getCompany() != null) {
            companyId = channel.getCompany().getId();
        } else if (visitor != null && visitor.getCompany() != null) {
            companyId = visitor.getCompany().getId();
        }

        return new ConversationResponse(
                conversation.getId(),
                conversation.getStatus(),
                conversation.getHandlingMode(),
                conversation.getAssigneeId(),
                visitor != null ? visitor.getId() : null,
                visitor != null ? visitor.getVisitorToken() : null,
                visitor != null ? visitor.getName() : null,
                visitor != null ? visitor.getEmail() : null,
                channel != null ? channel.getId() : null,
                channel != null ? channel.getName() : null,
                channel != null ? channel.getType() : null,
                companyId,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getResolvedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public HandlingMode getHandlingMode() {
        return handlingMode;
    }

    public void setHandlingMode(HandlingMode handlingMode) {
        this.handlingMode = handlingMode;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public String getVisitorToken() {
        return visitorToken;
    }

    public void setVisitorToken(String visitorToken) {
        this.visitorToken = visitorToken;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public String getVisitorEmail() {
        return visitorEmail;
    }

    public void setVisitorEmail(String visitorEmail) {
        this.visitorEmail = visitorEmail;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

