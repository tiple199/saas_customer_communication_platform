package vn.lnt.saas_customer_communication_platform.feature.superadmin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminDashboardResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminDashboardService;

@RestController
@RequestMapping("/api/v1/super-admin/dashboard")
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService superAdminDashboardService;

    public SuperAdminDashboardController(SuperAdminDashboardService superAdminDashboardService) {
        this.superAdminDashboardService = superAdminDashboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SuperAdminDashboardResponse>> getDashboardStats(
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserEmail = jwt.getSubject();
        SuperAdminDashboardResponse response = superAdminDashboardService.getDashboardStats(currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu bảng điều khiển Super Admin thành công", response));
    }
}

