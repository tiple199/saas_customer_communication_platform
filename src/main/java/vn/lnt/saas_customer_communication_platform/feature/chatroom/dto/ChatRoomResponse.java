package vn.lnt.saas_customer_communication_platform.feature.chatroom.dto;

import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoom;

import java.time.LocalDateTime;

public class ChatRoomResponse {

    private Long id;
    private String name;
    private String type;
    private Long createBy;
    private Long companyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Ảnh đại diện: với phòng DIRECT là avatar của người còn lại.
     */
    private String avatarUrl;

    /**
     * ID người còn lại trong phòng DIRECT (null với phòng GROUP).
     */
    private Long partnerUserId;

    public ChatRoomResponse() {
    }

    public ChatRoomResponse(Long id, String name, String type, Long createBy, Long companyId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.createBy = createBy;
        this.companyId = companyId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChatRoomResponse fromEntity(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getType() != null ? chatRoom.getType().name() : null,
                chatRoom.getCreateBy(),
                chatRoom.getCompany() != null ? chatRoom.getCompany().getId() : null,
                chatRoom.getCreatedAt(),
                chatRoom.getUpdatedAt()
        );
    }

    /**
     * Tạo response cho phòng DIRECT: {@code name} là tên người còn lại (tính theo người xem).
     */
    public static ChatRoomResponse fromEntity(ChatRoom chatRoom, User partner) {
        ChatRoomResponse response = fromEntity(chatRoom);
        if (partner != null) {
            response.setPartnerUserId(partner.getId());
            String partnerName = partner.getFullName() != null && !partner.getFullName().isBlank()
                    ? partner.getFullName()
                    : partner.getEmail();
            response.setName(partnerName);
            response.setAvatarUrl(partner.getAvatarUrl());
        }
        return response;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Long getPartnerUserId() {
        return partnerUserId;
    }

    public void setPartnerUserId(Long partnerUserId) {
        this.partnerUserId = partnerUserId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
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
}
