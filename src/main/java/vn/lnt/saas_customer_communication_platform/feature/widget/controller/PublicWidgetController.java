package vn.lnt.saas_customer_communication_platform.feature.widget.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeResponse;
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

    private String extractRequestOrigin(HttpServletRequest request) {
        String requestOrigin = request.getHeader("Origin");
        if (requestOrigin == null || requestOrigin.isBlank()) {
            requestOrigin = request.getHeader("Referer");
        }
        return requestOrigin;
    }
}
