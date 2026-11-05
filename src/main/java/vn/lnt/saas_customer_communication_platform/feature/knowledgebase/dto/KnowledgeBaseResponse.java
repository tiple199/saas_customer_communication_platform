package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto;

import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.KnowledgeBase;

import java.time.LocalDateTime;

public class KnowledgeBaseResponse {

    private Long id;
    private String name;
    private String description;
    private Long companyId;
    private String companyName;
    private long documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeBaseResponse() {
    }

    public KnowledgeBaseResponse(Long id, String name, String description, Long companyId,
                                 String companyName, long documentCount,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.companyId = companyId;
        this.companyName = companyName;
        this.documentCount = documentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static KnowledgeBaseResponse fromEntity(KnowledgeBase kb, long documentCount) {
        if (kb == null) {
            return null;
        }
        return new KnowledgeBaseResponse(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                kb.getCompany() != null ? kb.getCompany().getId() : null,
                kb.getCompany() != null ? kb.getCompany().getName() : null,
                documentCount,
                kb.getCreatedAt(),
                kb.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

