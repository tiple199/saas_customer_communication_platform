package vn.lnt.saas_customer_communication_platform.feature.chatroom.service;

import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.*;

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

    /**
     * Gửi tin nhắn vào phòng chat (DIRECT hoặc GROUP) và phát realtime qua WebSocket.
     */
    InternalMessageResponse sendMessage(String currentUserEmail, Long companyId, Long chatRoomId, SendMessageRequest request);

    /**
     * Lấy danh sách tin nhắn trong phòng chat theo phân trang.
     */
    PageResponse<InternalMessageResponse> getChatRoomMessages(String currentUserEmail, Long companyId, Long chatRoomId, int page, int size);

    /**
     * Đánh dấu đã đọc tin nhắn trong phòng chat.
     * Nếu messageId == null, sẽ đánh dấu đã đọc đến tin nhắn mới nhất trong phòng.
     */
    void markAsRead(String currentUserEmail, Long companyId, Long chatRoomId, Long messageId);
}

