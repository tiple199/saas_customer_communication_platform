package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.CreateKnowledgeBaseRequest;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.KnowledgeBaseResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.UpdateKnowledgeBaseRequest;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.Document;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.KnowledgeBase;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentChunkRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.KnowledgeBaseRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.KnowledgeBaseService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;

    public KnowledgeBaseServiceImpl(KnowledgeBaseRepository knowledgeBaseRepository,
                                    DocumentRepository documentRepository,
                                    DocumentChunkRepository documentChunkRepository,
                                    UserRepository userRepository,
                                    CompanyRepository companyRepository,
                                    CompanyMemberRepository companyMemberRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
    }

    @Override
    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(String currentUserEmail, Long companyId, CreateKnowledgeBaseRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        Company company = getCompanyByIdOrThrow(targetCompanyId);

        // Chỉ OWNER hoặc ADMIN mới có quyền tạo Cơ sở tri thức
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền tạo cơ sở tri thức");

        String trimmedName = request.getName().trim();
        if (knowledgeBaseRepository.existsByCompanyIdAndName(targetCompanyId, trimmedName)) {
            throw new InvalidOperationException("Tên cơ sở tri thức '" + trimmedName + "' đã tồn tại trong công ty này");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(trimmedName);
        kb.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        kb.setCompany(company);

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return KnowledgeBaseResponse.fromEntity(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> getKnowledgeBases(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        List<KnowledgeBase> list = knowledgeBaseRepository.findByCompanyIdOrderByCreatedAtDesc(targetCompanyId);
        return list.stream()
                .map(kb -> {
                    long docCount = documentRepository.countByKnowledgeBaseId(kb.getId());
                    return KnowledgeBaseResponse.fromEntity(kb, docCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<KnowledgeBaseResponse> getKnowledgeBasesPaged(String currentUserEmail, Long companyId,
                                                                      String search, Pageable pageable) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        int validPage = Math.max(pageable.getPageNumber(), 0);
        int validSize = pageable.getPageSize() <= 0 ? 20 : Math.min(pageable.getPageSize(), 100);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(validPage, validSize, sort);

        Page<KnowledgeBase> page;
        if (search != null && !search.isBlank()) {
            page = knowledgeBaseRepository.findByCompanyIdAndNameContainingIgnoreCase(targetCompanyId, search.trim(), safePageable);
        } else {
            page = knowledgeBaseRepository.findByCompanyId(targetCompanyId, safePageable);
        }

        List<KnowledgeBaseResponse> items = page.getContent().stream()
                .map(kb -> {
                    long docCount = documentRepository.countByKnowledgeBaseId(kb.getId());
                    return KnowledgeBaseResponse.fromEntity(kb, docCount);
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
    public KnowledgeBaseResponse getKnowledgeBaseById(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", "id", id));

        long docCount = documentRepository.countByKnowledgeBaseId(kb.getId());
        return KnowledgeBaseResponse.fromEntity(kb, docCount);
    }

    @Override
    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(String currentUserEmail, Long companyId, Long id, UpdateKnowledgeBaseRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền cập nhật cơ sở tri thức");

        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", "id", id));

        String trimmedName = request.getName().trim();
        if (knowledgeBaseRepository.existsByCompanyIdAndNameAndIdNot(targetCompanyId, trimmedName, id)) {
            throw new InvalidOperationException("Tên cơ sở tri thức '" + trimmedName + "' đã được sử dụng bởi cơ sở tri thức khác");
        }

        kb.setName(trimmedName);
        kb.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        long docCount = documentRepository.countByKnowledgeBaseId(saved.getId());
        return KnowledgeBaseResponse.fromEntity(saved, docCount);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền xóa cơ sở tri thức");

        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", "id", id));

        // Xóa sạch các chunks và documents thuộc KnowledgeBase này
        List<Document> documents = documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(id);
        if (!documents.isEmpty()) {
            List<Long> docIds = documents.stream().map(Document::getId).collect(Collectors.toList());
            documentChunkRepository.deleteByDocumentIdIn(docIds);
            documentRepository.deleteAll(documents);
        }

        knowledgeBaseRepository.delete(kb);
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

