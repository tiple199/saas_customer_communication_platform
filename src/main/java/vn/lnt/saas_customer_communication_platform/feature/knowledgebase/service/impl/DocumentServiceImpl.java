package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentChunkSearchResult;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentPreviewResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.embedding.EmbeddingService;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.Document;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.DocumentChunk;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.DocumentStatus;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.KnowledgeBase;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.DocumentParserManager;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.TextChunker;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentChunkRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.KnowledgeBaseRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.DocumentService;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanLimitEnforcementService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final DocumentParserManager documentParserManager;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final PlanLimitEnforcementService planLimitEnforcementService;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               DocumentChunkRepository documentChunkRepository,
                               KnowledgeBaseRepository knowledgeBaseRepository,
                               FileStorageService fileStorageService,
                               UserRepository userRepository,
                               CompanyRepository companyRepository,
                               CompanyMemberRepository companyMemberRepository,
                               DocumentParserManager documentParserManager,
                               TextChunker textChunker,
                               EmbeddingService embeddingService,
                               PlanLimitEnforcementService planLimitEnforcementService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.documentParserManager = documentParserManager;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.planLimitEnforcementService = planLimitEnforcementService;
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(String currentUserEmail, Long companyId, Long knowledgeBaseId, MultipartFile file) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        Company company = getCompanyByIdOrThrow(targetCompanyId);

        // Chỉ OWNER hoặc ADMIN mới có quyền tải lên tài liệu vào cơ sở tri thức
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền tải lên tài liệu");

        // Enforce Plan Limits: storageLimit (MB)
        planLimitEnforcementService.validateCanUploadDocument(targetCompanyId, file != null ? file.getSize() : 0L);

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndCompanyId(knowledgeBaseId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", "id", knowledgeBaseId));

        String subDirectory = "kb_" + knowledgeBaseId;
        StoredFileInfo storedFileInfo = fileStorageService.storeFile(file, subDirectory);

        Document document = new Document();
        document.setFileName(storedFileInfo.getOriginalFileName());
        document.setFileUrl(storedFileInfo.getStoredFileName());
        document.setFileType(storedFileInfo.getFileType());
        document.setFileSize(storedFileInfo.getFileSize());
        document.setStatus(DocumentStatus.UPLOADED);
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
    @Transactional
    public DocumentResponse processDocument(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền xử lý tài liệu");

        Document document = documentRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        document.setStatus(DocumentStatus.PROCESSING);
        document = documentRepository.save(document);

        Long kbId = document.getKnowledgeBase() != null ? document.getKnowledgeBase().getId() : null;
        String subDirectory = kbId != null ? "kb_" + kbId : "";

        try {
            Resource resource = fileStorageService.loadFileAsResource(document.getFileUrl(), subDirectory);
            String extractedText = documentParserManager.parseDocument(resource.getInputStream(), document.getFileType());

            if (extractedText == null || extractedText.isBlank()) {
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.save(document);
                throw new InvalidOperationException("Tài liệu không chứa nội dung chữ có thể bóc tách");
            }

            // Xóa sạch các chunks cũ (nếu có)
            documentChunkRepository.deleteByDocumentId(document.getId());

            // Băm nhỏ thành các chunks
            List<String> rawChunks = textChunker.splitIntoChunks(extractedText);

            // Sinh Vector Embedding cho từng chunk thông qua Google Gemini
            List<List<Float>> embeddings = embeddingService.generateEmbeddings(rawChunks);

            for (int i = 0; i < rawChunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(document);
                chunk.setCompany(document.getCompany());
                chunk.setChunkIndex(i);
                chunk.setContent(rawChunks.get(i));
                if (embeddings != null && i < embeddings.size()) {
                    chunk.setEmbedding(embeddingService.serializeEmbedding(embeddings.get(i)));
                }
                documentChunkRepository.save(chunk);
            }

            document.setStatus(DocumentStatus.READY);
            Document saved = documentRepository.save(document);
            return DocumentResponse.fromEntity(saved, rawChunks.size());
        } catch (Exception ex) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new InvalidOperationException("Lỗi khi xử lý bóc tách tài liệu: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkSearchResult> searchSimilarChunks(String currentUserEmail, Long companyId,
                                                                Long knowledgeBaseId, String query, int topK, double minScore) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        return retrieveSimilarChunksByCompany(targetCompanyId, knowledgeBaseId, query, topK, minScore);
    }

    @Override
    @Transactional
    public List<DocumentChunkSearchResult> retrieveSimilarChunksByCompany(Long companyId, Long knowledgeBaseId,
                                                                          String query, int topK, double minScore) {
        if (companyId == null || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<DocumentChunk> chunks = (knowledgeBaseId != null)
                ? documentChunkRepository.findByCompanyIdAndKnowledgeBaseId(companyId, knowledgeBaseId)
                : documentChunkRepository.findReadyChunksByCompanyId(companyId);

        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        // Tạo embedding cho câu hỏi truy vấn
        List<Float> queryVector;
        try {
            queryVector = embeddingService.generateEmbedding(query);
        } catch (Exception ex) {
            log.warn("[Semantic Search] Không thể tạo embedding cho truy vấn \"{}\": {}", query, ex.getMessage());
            return Collections.emptyList();
        }
        if (queryVector == null || queryVector.isEmpty()) {
            return Collections.emptyList();
        }

        // Tự động cập nhật lại vector cho các chunk cũ nếu chưa có embedding hoặc lệch số chiều vector
        List<DocumentChunk> chunksNeedingEmbedding = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                chunksNeedingEmbedding.add(chunk);
            } else {
                List<Float> existingVec = embeddingService.parseEmbedding(chunk.getEmbedding());
                if (existingVec.size() != queryVector.size()) {
                    chunksNeedingEmbedding.add(chunk);
                }
            }
        }
        if (!chunksNeedingEmbedding.isEmpty()) {
            try {
                log.info("[Semantic Search] Tự động đồng bộ lại vector embedding ({} chiều) cho {} phân đoạn tài liệu của công ty #{}",
                        queryVector.size(), chunksNeedingEmbedding.size(), companyId);
                List<String> textsToEmbed = chunksNeedingEmbedding.stream()
                        .map(DocumentChunk::getContent)
                        .collect(Collectors.toList());
                List<List<Float>> newVectors = embeddingService.generateEmbeddings(textsToEmbed);
                for (int i = 0; i < chunksNeedingEmbedding.size() && i < newVectors.size(); i++) {
                    DocumentChunk chunk = chunksNeedingEmbedding.get(i);
                    chunk.setEmbedding(embeddingService.serializeEmbedding(newVectors.get(i)));
                    documentChunkRepository.save(chunk);
                }
            } catch (Exception ex) {
                log.warn("[Semantic Search] Không thể đồng bộ lại embedding cho các chunk cũ: {}", ex.getMessage());
            }
        }

        List<DocumentChunkSearchResult> results = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                continue;
            }
            List<Float> chunkVector = embeddingService.parseEmbedding(chunk.getEmbedding());
            if (chunkVector.isEmpty()) {
                continue;
            }
            double score = embeddingService.calculateCosineSimilarity(queryVector, chunkVector);
            if (score >= minScore) {
                String docName = chunk.getDocument() != null ? chunk.getDocument().getFileName() : "";
                Long docId = chunk.getDocument() != null ? chunk.getDocument().getId() : null;
                results.add(new DocumentChunkSearchResult(
                        chunk.getId(),
                        docId,
                        docName,
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        score
                ));
            }
        }

        results.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        int limit = topK > 0 ? Math.min(topK, results.size()) : results.size();
        return new ArrayList<>(results.subList(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentPreviewResponse getDocumentPreview(String currentUserEmail, Long companyId, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Document document = documentRepository.findByIdAndCompanyId(id, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        long chunkCount = documentChunkRepository.countByDocumentId(document.getId());

        String fileType = document.getFileType() != null ? document.getFileType().toLowerCase().trim() : "";
        boolean isPdf = fileType.equals("pdf");
        boolean isText = fileType.equals("txt") || fileType.equals("md") || fileType.equals("csv") || fileType.equals("json");
        boolean isOffice = fileType.equals("doc") || fileType.equals("docx") || fileType.equals("xls") || fileType.equals("xlsx");

        String textContent = null;
        boolean truncated = false;

        // Nếu tài liệu đã được bóc tách READY thành chunks, tải trực tiếp text từ chunks
        if (document.getStatus() == DocumentStatus.READY && chunkCount > 0) {
            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId());
            if (!chunks.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int totalLen = 0;
                for (DocumentChunk chunk : chunks) {
                    if (totalLen + chunk.getContent().length() > 60000) {
                        truncated = true;
                        break;
                    }
                    if (sb.length() > 0) sb.append("\n\n---\n\n");
                    sb.append(chunk.getContent());
                    totalLen = sb.length();
                }
                textContent = sb.toString();
                isText = true;
            }
        } else if (isText) {
            Long kbId = document.getKnowledgeBase() != null ? document.getKnowledgeBase().getId() : null;
            String subDirectory = kbId != null ? "kb_" + kbId : "";
            int maxChars = 60000;
            textContent = fileStorageService.readTextPreview(document.getFileUrl(), subDirectory, maxChars);
            if (textContent != null && textContent.length() >= maxChars) {
                truncated = true;
            }
        }

        return new DocumentPreviewResponse(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus() != null ? document.getStatus().name() : null,
                chunkCount,
                document.getKnowledgeBase() != null ? document.getKnowledgeBase().getId() : null,
                document.getKnowledgeBase() != null ? document.getKnowledgeBase().getName() : null,
                document.getCompany() != null ? document.getCompany().getId() : null,
                document.getCreatedAt(),
                textContent,
                isText,
                isPdf,
                isOffice,
                truncated
        );
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

    @Override
    @Transactional
    public void deleteDocumentsBatch(String currentUserEmail, Long companyId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền xóa tài liệu");

        for (Long docId : documentIds) {
            documentRepository.findByIdAndCompanyId(docId, targetCompanyId).ifPresent(doc -> {
                documentChunkRepository.deleteByDocumentId(docId);
                Long kbId = doc.getKnowledgeBase() != null ? doc.getKnowledgeBase().getId() : null;
                String subDirectory = kbId != null ? "kb_" + kbId : "";
                fileStorageService.deleteFile(doc.getFileUrl(), subDirectory);
                documentRepository.delete(doc);
            });
        }
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

