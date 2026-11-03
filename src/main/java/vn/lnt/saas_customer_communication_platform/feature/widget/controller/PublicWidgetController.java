package vn.lnt.saas_customer_communication_platform.feature.widget.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetSendMessageRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.service.WidgetService;

@RestController
@RequestMapping("/api/v1/public/widget")
public class PublicWidgetController {

    private final WidgetService widgetService;

    public PublicWidgetController(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PublicWidgetConfigResponse>> getWidgetConfig(
            @RequestParam String channelKey,
            HttpServletRequest request) {
        String requestOrigin = extractRequestOrigin(request);
        PublicWidgetConfigResponse response = widgetService.getWidgetConfig(channelKey, requestOrigin);
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình Widget thành công", response));
    }

    @PostMapping("/handshake")
    public ResponseEntity<ApiResponse<WidgetHandshakeResponse>> handshake(
            @Valid @RequestBody WidgetHandshakeRequest request,
            HttpServletRequest httpRequest) {
        String requestOrigin = extractRequestOrigin(httpRequest);
        WidgetHandshakeResponse response = widgetService.handshake(request, requestOrigin);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo kết nối Widget thành công", response));
    }

    @GetMapping("/conversations/restore")
    public ResponseEntity<ApiResponse<RestoreConversationResponse>> restoreConversation(
            @RequestParam String channelKey,
            @RequestParam String visitorToken,
            HttpServletRequest request) {
        String requestOrigin = extractRequestOrigin(request);
        RestoreConversationResponse response = widgetService.restoreConversation(channelKey, visitorToken, requestOrigin);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục cuộc hội thoại thành công", response));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ConversationMessageResponse>> sendMessage(
            @Valid @RequestBody WidgetSendMessageRequest request,
            HttpServletRequest httpRequest) {
        String requestOrigin = extractRequestOrigin(httpRequest);
        ConversationMessageResponse response = widgetService.sendMessage(request, requestOrigin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Gửi tin nhắn thành công", response));
    }

    private String extractRequestOrigin(HttpServletRequest request) {
        String requestOrigin = request.getHeader("Origin");
        if (requestOrigin == null || requestOrigin.isBlank()) {
            requestOrigin = request.getHeader("Referer");
        }
        return requestOrigin;
    }
}
