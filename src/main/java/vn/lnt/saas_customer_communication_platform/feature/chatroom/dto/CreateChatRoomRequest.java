package vn.lnt.saas_customer_communication_platform.feature.chatroom.dto;

import jakarta.validation.constraints.NotNull;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoomType;

import java.util.List;

public class CreateChatRoomRequest {

    private String name;

    @NotNull(message = "Loại phòng chat không được để trống (DIRECT hoặc GROUP)")
    private ChatRoomType type;

    private List<Long> memberUserIds;

    public CreateChatRoomRequest() {
    }

    public CreateChatRoomRequest(String name, ChatRoomType type, List<Long> memberUserIds) {
        this.name = name;
        this.type = type;
        this.memberUserIds = memberUserIds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChatRoomType getType() {
        return type;
    }

    public void setType(ChatRoomType type) {
        this.type = type;
    }

    public List<Long> getMemberUserIds() {
        return memberUserIds;
    }

    public void setMemberUserIds(List<Long> memberUserIds) {
        this.memberUserIds = memberUserIds;
    }
}
