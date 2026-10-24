package vn.lnt.saas_customer_communication_platform.feature.chatroom.dto;

import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoomMember;

import java.time.LocalDateTime;

public class ChatRoomMemberResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private LocalDateTime joinAt;
    private Long lastReadMessageId;

    public ChatRoomMemberResponse() {
    }

    public ChatRoomMemberResponse(Long id, Long userId, String fullName, String email, String avatarUrl, LocalDateTime joinAt) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.joinAt = joinAt;
    }

    public static ChatRoomMemberResponse fromEntity(ChatRoomMember member, User user) {
        ChatRoomMemberResponse response = new ChatRoomMemberResponse(
                member.getId(),
                member.getUserId(),
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getAvatarUrl() : null,
                member.getJoinAt()
        );
        response.setLastReadMessageId(member.getLastReadMessageId());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDateTime getJoinAt() {
        return joinAt;
    }

    public void setJoinAt(LocalDateTime joinAt) {
        this.joinAt = joinAt;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
