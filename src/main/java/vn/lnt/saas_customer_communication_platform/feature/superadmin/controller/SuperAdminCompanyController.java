package vn.lnt.saas_customer_communication_platform.feature.superadmin.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminCompanyService;

@RestController
@RequestMapping("/api/v1/super-admin/companies")
public class SuperAdminCompanyController {

    private final SuperAdminCompanyService superAdminCompanyService;

    public SuperAdminCompanyController(SuperAdminCompanyService superAdminCompanyService) {
        this.superAdminCompanyService = superAdminCompanyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SuperAdminCompanyItemDto>>> getCompanies(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long planId,
            Pageable pageable) {

        String currentUserEmail = jwt.getSubject();
        PageResponse<SuperAdminCompanyItemDto> response = superAdminCompanyService.getCompanies(
                currentUserEmail, keyword, status, planId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách doanh nghiệp thành công", response));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<SuperAdminCompanyDetailDto>> getCompanyDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminCompanyDetailDto response = superAdminCompanyService.getCompanyDetail(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết doanh nghiệp thành công", response));
    }

    @PutMapping("/{companyId}/status")
    public ResponseEntity<ApiResponse<SuperAdminCompanyItemDto>> updateCompanyStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @Valid @RequestBody UpdateCompanyStatusRequest request) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminCompanyItemDto response = superAdminCompanyService.updateCompanyStatus(
                currentUserEmail, companyId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái doanh nghiệp thành công", response));
    }

    @PostMapping("/{companyId}/assign-plan")
    public ResponseEntity<ApiResponse<SuperAdminCompanyDetailDto>> assignPlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @Valid @RequestBody SuperAdminAssignPlanRequest request) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminCompanyDetailDto response = superAdminCompanyService.assignPlan(
                currentUserEmail, companyId, request);
        return ResponseEntity.ok(ApiResponse.success("Gán gói cước cho doanh nghiệp thành công", response));
    }
}

