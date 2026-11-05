package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.controller;

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
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.CreateKnowledgeBaseRequest;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.KnowledgeBaseResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.UpdateKnowledgeBaseRequest;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.KnowledgeBaseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> createKnowledgeBase(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(currentUserEmail, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo cơ sở tri thức thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeBaseResponse>>> getKnowledgeBases(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<KnowledgeBaseResponse> response = knowledgeBaseService.getKnowledgeBases(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cơ sở tri thức thành công", response));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeBaseResponse>>> getKnowledgeBasesPaged(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PageResponse<KnowledgeBaseResponse> response = knowledgeBaseService.getKnowledgeBasesPaged(currentUserEmail, companyId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cơ sở tri thức thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> getKnowledgeBaseById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBaseById(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cơ sở tri thức thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> updateKnowledgeBase(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        KnowledgeBaseResponse response = knowledgeBaseService.updateKnowledgeBase(currentUserEmail, companyId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cơ sở tri thức thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledgeBase(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        knowledgeBaseService.deleteKnowledgeBase(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cơ sở tri thức thành công", null));
    }
}

