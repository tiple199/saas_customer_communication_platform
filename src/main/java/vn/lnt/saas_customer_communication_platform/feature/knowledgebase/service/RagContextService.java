package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service;

import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.RagContextResponse;

public interface RagContextService {

    /**
     * Xây dựng ngữ cảnh RAG (dành cho người dùng đăng nhập / Admin kiểm thử).
     */
    RagContextResponse buildContext(String currentUserEmail, Long companyId, Long knowledgeBaseId,
                                    Long conversationId, String query, int topK, double minScore);

    /**
     * Xây dựng ngữ cảnh RAG nội bộ theo companyId (dành cho luồng Auto-Reply AI khi khách hàng nhắn tin).
     */
    RagContextResponse buildContextForCompany(Long companyId, Long knowledgeBaseId,
                                              Long conversationId, String query, int topK, double minScore);
}

