package vn.lnt.saas_customer_communication_platform.feature.company.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateCompanyRequest;
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
}
