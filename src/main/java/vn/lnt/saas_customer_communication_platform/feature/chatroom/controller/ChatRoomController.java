package vn.lnt.saas_customer_communication_platform.feature.chatroom.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.ChatRoomMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.ChatRoomResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.CreateChatRoomRequest;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.service.ChatRoomService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @Valid @RequestBody CreateChatRoomRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChatRoomResponse response = chatRoomService.createChatRoom(currentUserEmail, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo phòng chat thành công", response));
    }

    @PostMapping("/direct/{targetUserId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getOrCreateDirectRoom(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long targetUserId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChatRoomResponse response = chatRoomService.getOrCreateDirectRoom(currentUserEmail, companyId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy phòng chat 1-1 thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyChatRooms(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<ChatRoomResponse> response = chatRoomService.getMyChatRooms(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng chat thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoomById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChatRoomResponse response = chatRoomService.getChatRoomById(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng chat thành công", response));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<ChatRoomMemberResponse>>> getChatRoomMembers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<ChatRoomMemberResponse> response = chatRoomService.getChatRoomMembers(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành viên phòng chat thành công", response));
    }
}
