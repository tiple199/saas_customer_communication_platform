package vn.lnt.saas_customer_communication_platform.feature.widget.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
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
        // Lấy origin từ header Origin hoặc Referer của trình duyệt gửi lên
        String requestOrigin = request.getHeader("Origin");
        if (requestOrigin == null || requestOrigin.isBlank()) {
            requestOrigin = request.getHeader("Referer");
        }

        PublicWidgetConfigResponse response = widgetService.getWidgetConfig(channelKey, requestOrigin);
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình Widget thành công", response));
    }
}
