package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentResponse;

public interface DocumentService {

    DocumentResponse uploadDocument(String currentUserEmail, Long companyId, Long knowledgeBaseId, MultipartFile file);

    PageResponse<DocumentResponse> getDocumentsByKnowledgeBase(String currentUserEmail, Long companyId,
                                                               Long knowledgeBaseId, String search, Pageable pageable);

    DocumentResponse getDocumentById(String currentUserEmail, Long companyId, Long id);

    Resource downloadDocument(String currentUserEmail, Long companyId, Long id);

    void deleteDocument(String currentUserEmail, Long companyId, Long id);
}

