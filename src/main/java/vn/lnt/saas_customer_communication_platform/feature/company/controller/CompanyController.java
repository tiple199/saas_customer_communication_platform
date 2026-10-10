package vn.lnt.saas_customer_communication_platform.feature.company.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.AddMemberRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateMemberAccountRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateMemberStatusRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCompanyRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyResponse response = companyService.createCompany(currentUserEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo công ty thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getMyCompanies(@AuthenticationPrincipal Jwt jwt) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<CompanyResponse> response = companyService.getMyCompanies(currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách công ty thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyResponse response = companyService.getCompanyById(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin công ty thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyResponse response = companyService.updateCompany(currentUserEmail, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin công ty thành công", response));
    }

    @PostMapping("/{id}/switch")
    public ResponseEntity<ApiResponse<CompanyResponse>> switchCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyResponse response = companyService.switchCompany(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Chuyển công ty thành công", response));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<CompanyMemberResponse>>> getCompanyMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<CompanyMemberResponse> response = companyService.getCompanyMembers(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành viên công ty thành công", response));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<CompanyMemberResponse>> addMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyMemberResponse response = companyService.addMember(currentUserEmail, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm thành viên thành công", response));
    }

    @DeleteMapping("/{id}/members/{memberUserId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @PathVariable Long memberUserId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        companyService.removeMember(currentUserEmail, id, memberUserId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành viên khỏi công ty thành công", null));
    }

    @PatchMapping("/{id}/members/{memberUserId}/status")
    public ResponseEntity<ApiResponse<CompanyMemberResponse>> updateMemberStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @PathVariable Long memberUserId,
            @Valid @RequestBody UpdateMemberStatusRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyMemberResponse response = companyService.updateMemberStatus(currentUserEmail, id, memberUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành viên thành công", response));
    }

    @PostMapping("/{id}/members/create-account")
    public ResponseEntity<ApiResponse<CompanyMemberResponse>> createMemberAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody CreateMemberAccountRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        CompanyMemberResponse response = companyService.createMemberAccount(currentUserEmail, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo tài khoản và thêm thành viên thành công", response));
    }
}
