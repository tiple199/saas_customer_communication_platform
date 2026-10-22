package vn.lnt.saas_customer_communication_platform.feature.chatroom.service;

import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.AddChatRoomMemberRequest;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.ChatRoomMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.ChatRoomResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.CreateChatRoomRequest;

import java.util.List;

public interface ChatRoomService {

    ChatRoomResponse createChatRoom(String currentUserEmail, Long companyId, CreateChatRoomRequest request);

    List<ChatRoomResponse> getMyChatRooms(String currentUserEmail, Long companyId);

    ChatRoomResponse getChatRoomById(String currentUserEmail, Long companyId, Long chatRoomId);

    List<ChatRoomMemberResponse> getChatRoomMembers(String currentUserEmail, Long companyId, Long chatRoomId);

    /**
     * Lấy phòng chat DIRECT (1-1) giữa người dùng hiện tại và {@code targetUserId}
     * trong công ty; nếu chưa tồn tại thì tạo mới.
     */
    ChatRoomResponse getOrCreateDirectRoom(String currentUserEmail, Long companyId, Long targetUserId);

    /**
     * Thêm thành viên vào phòng chat nhóm (GROUP).
     */
    void addMembers(String currentUserEmail, Long companyId, Long chatRoomId, AddChatRoomMemberRequest request);

    /**
     * Xóa thành viên khỏi phòng chat nhóm (GROUP), hoặc thành viên tự rời nhóm.
     */
    void removeMember(String currentUserEmail, Long companyId, Long chatRoomId, Long targetUserId);
}

