package vn.lnt.saas_customer_communication_platform.feature.chatroom.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AddChatRoomMemberRequest {

    @NotEmpty(message = "Danh sách thành viên không được để trống")
    private List<Long> userIds;

    public AddChatRoomMemberRequest() {
    }

    public AddChatRoomMemberRequest(List<Long> userIds) {
        this.userIds = userIds;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
