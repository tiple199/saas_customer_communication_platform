package vn.lnt.saas_customer_communication_platform.feature.conversation.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.AssignConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.CreateConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.SendConversationMessageRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.UpdateConversationStatusRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.ConversationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @Valid @RequestBody CreateConversationRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ConversationResponse response = conversationService.createConversationForCompany(currentUserEmail, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo cuộc hội thoại thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<ConversationResponse> response = conversationService.getConversations(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cuộc hội thoại thành công", response));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> getConversationsPaged(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) ConversationStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PageResponse<ConversationResponse> response = conversationService.getConversationsPaged(
                currentUserEmail, companyId, status, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cuộc hội thoại thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ConversationResponse response = conversationService.getConversationById(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cuộc hội thoại thành công", response));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<RestoreConversationResponse>> restoreConversation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        RestoreConversationResponse response = conversationService.restoreConversationById(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục cuộc hội thoại thành công", response));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<ConversationMessageResponse>>> getConversationMessages(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<ConversationMessageResponse> response = conversationService.getMessagesByConversationId(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin nhắn thành công", response));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<ConversationMessageResponse>> sendStaffMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody SendConversationMessageRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ConversationMessageResponse response = conversationService.sendStaffMessage(currentUserEmail, companyId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Gửi tin nhắn trả lời thành công", response));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<ConversationResponse>> assignStaff(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id,
            @RequestBody(required = false) AssignConversationRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        Long assigneeId = request != null ? request.getAssigneeId() : null;
        ConversationResponse response = conversationService.assignStaff(currentUserEmail, companyId, id, assigneeId);
        return ResponseEntity.ok(ApiResponse.success("Gán nhân viên phụ trách cuộc hội thoại thành công", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ConversationResponse>> updateConversationStatus(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationStatusRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ConversationResponse response = conversationService.updateConversationStatus(
                currentUserEmail, companyId, id, request.getStatus()
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái cuộc hội thoại thành công", response));
    }
}

