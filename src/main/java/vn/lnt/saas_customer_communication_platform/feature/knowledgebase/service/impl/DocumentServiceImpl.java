package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.impl;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.lnt.saas_customer_communication_platform.common.storage.FileStorageService;
import vn.lnt.saas_customer_communication_platform.common.storage.StoredFileInfo;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.Document;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.KnowledgeBase;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentChunkRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.KnowledgeBaseRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.DocumentService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               DocumentChunkRepository documentChunkRepository,
                               KnowledgeBaseRepository knowledgeBaseRepository,
                               FileStorageService fileStorageService,
                               UserRepository userRepository,
                               CompanyRepository companyRepository,
                               CompanyMemberRepository companyMemberRepository) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(String currentUserEmail, Long companyId, Long knowledgeBaseId, MultipartFile file) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        Company company = getCompanyByIdOrThrow(targetCompanyId);

        // Chỉ OWNER hoặc ADMIN mới có quyền tải lên tài liệu vào cơ sở tri thức
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền tải lên tài liệu");

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndCompanyId(knowledgeBaseId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", "id", knowledgeBaseId));

        String subDirectory = "kb_" + knowledgeBaseId;
        StoredFileInfo storedFileInfo = fileStorageService.storeFile(file, subDirectory);

        Document document = new Document();
        document.setFileName(storedFileInfo.getOriginalFileName());
        document.setFileUrl(storedFileInfo.getStoredFileName());
        document.setFileType(storedFileInfo.getFileType());
        document.setFileSize(storedFileInfo.getFileSize());
        document.setStatus("UPLOADED");
        document.setCompany(company);
        document.setKnowledgeBase(knowledgeBase);

        Document saved = documentRepository.save(document);
        return DocumentResponse.fromEntity(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> getDocumentsByKnowledgeBase(String currentUserEmail, Long companyId,
                                                                      Long knowledgeBaseId, String search, Pageable pageable) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        // Kiểm tra KnowledgeBase có thuộc công ty này không
        knowledgeBaseRepository.findByIdAndCompanyId(knowledgeBaseId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", "id", knowledgeBaseId));

        int validPage = Math.max(pageable.getPageNumber(), 0);
        int validSize = pageable.getPageSize() <= 0 ? 20 : Math.min(pageable.getPageSize(), 100);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(validPage, validSize, sort);

        Page<Document> page;
        if (search != null && !search.isBlank()) {
            page = documentRepository.findByKnowledgeBaseIdAndFileNameContainingIgnoreCase(knowledgeBaseId, search.trim(), safePageable);
        } else {
            page = documentRepository.findByKnowledgeBaseId(knowledgeBaseId, safePageable);
        }

        List<DocumentResponse> items = page.getContent().stream()
                .map(doc -> {
                    long chunkCount = documentChunkRepository.countByDocumentId(doc.getId());
                    return DocumentResponse.fromEntity(doc, chunkCount);
                })
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Document document = documentRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        long chunkCount = documentChunkRepository.countByDocumentId(document.getId());
        return DocumentResponse.fromEntity(document, chunkCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocument(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Document document = documentRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        Long kbId = document.getKnowledgeBase() != null ? document.getKnowledgeBase().getId() : null;
        String subDirectory = kbId != null ? "kb_" + kbId : "";

        return fileStorageService.loadFileAsResource(document.getFileUrl(), subDirectory);
    }

    @Override
    @Transactional
    public void deleteDocument(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền xóa tài liệu");

        Document document = documentRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        // 1. Xóa các chunks liên quan
        documentChunkRepository.deleteByDocumentId(id);

        // 2. Xóa file vật lý
        Long kbId = document.getKnowledgeBase() != null ? document.getKnowledgeBase().getId() : null;
        String subDirectory = kbId != null ? "kb_" + kbId : "";
        fileStorageService.deleteFile(document.getFileUrl(), subDirectory);

        // 3. Xóa bản ghi trong database
        documentRepository.delete(document);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Company getCompanyByIdOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));
    }

    private Long resolveTargetCompanyId(Long companyId) {
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }
        return targetCompanyId;
    }

    private void validateActiveCompanyMember(Long userId, Long companyId) {
        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(userId, companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));

        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }
    }

    private void validateActiveAdminOrOwner(Long userId, Long companyId, String actionErrorMessage) {
        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(userId, companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));

        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }

        if (member.getRole() != CompanyMemberRole.OWNER && member.getRole() != CompanyMemberRole.ADMIN) {
            throw new InvalidOperationException(actionErrorMessage);
        }
    }
}

