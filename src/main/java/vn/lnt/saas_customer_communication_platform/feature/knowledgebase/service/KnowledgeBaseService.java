package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.CreateKnowledgeBaseRequest;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.KnowledgeBaseResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.UpdateKnowledgeBaseRequest;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeBaseResponse createKnowledgeBase(String currentUserEmail, Long companyId, CreateKnowledgeBaseRequest request);

    List<KnowledgeBaseResponse> getKnowledgeBases(String currentUserEmail, Long companyId);

    PageResponse<KnowledgeBaseResponse> getKnowledgeBasesPaged(String currentUserEmail, Long companyId, String search, Pageable pageable);

    KnowledgeBaseResponse getKnowledgeBaseById(String currentUserEmail, Long companyId, Long id);

    KnowledgeBaseResponse updateKnowledgeBase(String currentUserEmail, Long companyId, Long id, UpdateKnowledgeBaseRequest request);

    void deleteKnowledgeBase(String currentUserEmail, Long companyId, Long id);
}

