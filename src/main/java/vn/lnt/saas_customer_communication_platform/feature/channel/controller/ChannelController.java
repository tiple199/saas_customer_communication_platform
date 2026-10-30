package vn.lnt.saas_customer_communication_platform.feature.channel.controller;

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
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.ChannelResponse;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelAiRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.service.ChannelService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @Valid @RequestBody CreateChannelRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChannelResponse response = channelService.createChannel(currentUserEmail, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo kênh kết nối thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getChannels(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<ChannelResponse> response = channelService.getChannels(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kênh kết nối thành công", response));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<ChannelResponse>>> getChannelsPaged(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PageResponse<ChannelResponse> response = channelService.getChannelsPaged(currentUserEmail, companyId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kênh kết nối thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannelById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChannelResponse response = channelService.getChannelById(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin kênh kết nối thành công", response));
    }

    @GetMapping("/key/{channelKey}")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannelByKey(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable String channelKey) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChannelResponse response = channelService.getChannelByKey(currentUserEmail, companyId, channelKey);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin kênh kết nối thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateChannelRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChannelResponse response = channelService.updateChannel(currentUserEmail, companyId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kênh kết nối thành công", response));
    }

    @PatchMapping("/{id}/ai")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannelAi(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateChannelAiRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChannelResponse response = channelService.updateChannelAi(currentUserEmail, companyId, id, request.getEnabled());
        String message = Boolean.TRUE.equals(request.getEnabled())
                ? "Bật tính năng AI cho kênh kết nối thành công"
                : "Tắt tính năng AI cho kênh kết nối thành công";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/{id}/regenerate-key")
    public ResponseEntity<ApiResponse<ChannelResponse>> regenerateChannelKey(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        ChannelResponse response = channelService.regenerateChannelKey(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Tạo lại khóa kênh kết nối thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        channelService.deleteChannel(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa kênh kết nối thành công", null));
    }
}
