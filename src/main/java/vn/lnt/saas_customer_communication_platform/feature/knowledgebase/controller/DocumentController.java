package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.controller;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentPreviewResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.DocumentService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        DocumentResponse response = documentService.uploadDocument(currentUserEmail, companyId, knowledgeBaseId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tải lên tài liệu thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> getDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PageResponse<DocumentResponse> response = documentService.getDocumentsByKnowledgeBase(
                currentUserEmail, companyId, knowledgeBaseId, search, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài liệu thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        DocumentResponse response = documentService.getDocumentById(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài liệu thành công", response));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<DocumentPreviewResponse>> getDocumentPreview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        DocumentPreviewResponse response = documentService.getDocumentPreview(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy bản xem trước tài liệu thành công", response));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        DocumentResponse doc = documentService.getDocumentById(currentUserEmail, companyId, id);
        Resource resource = documentService.downloadDocument(currentUserEmail, companyId, id);

        MediaType mediaType = MediaTypeFactory.getMediaType(doc.getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        String encodedFileName = URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        DocumentResponse doc = documentService.getDocumentById(currentUserEmail, companyId, id);
        Resource resource = documentService.downloadDocument(currentUserEmail, companyId, id);

        String encodedFileName = URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<DocumentResponse>> processDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        DocumentResponse response = documentService.processDocument(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Bóc tách nội dung tài liệu và phân đoạn thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        documentService.deleteDocument(currentUserEmail, companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài liệu thành công", null));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentsBatch(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @RequestBody List<Long> documentIds) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        documentService.deleteDocumentsBatch(currentUserEmail, companyId, documentIds);
        return ResponseEntity.ok(ApiResponse.success("Xóa các tài liệu đã chọn thành công", null));
    }
}

