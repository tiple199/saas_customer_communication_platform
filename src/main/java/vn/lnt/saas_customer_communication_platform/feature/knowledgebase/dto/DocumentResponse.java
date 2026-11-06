package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto;

import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.Document;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String status;
    private long chunkCount;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long companyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {
    }

    public DocumentResponse(Long id, String fileName, String fileUrl, String fileType, Long fileSize,
                            String status, long chunkCount, Long knowledgeBaseId, String knowledgeBaseName,
                            Long companyId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.chunkCount = chunkCount;
        this.knowledgeBaseId = knowledgeBaseId;
        this.knowledgeBaseName = knowledgeBaseName;
        this.companyId = companyId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentResponse fromEntity(Document doc, long chunkCount) {
        if (doc == null) {
            return null;
        }
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getFileUrl(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getStatus() != null ? doc.getStatus().name() : null,
                chunkCount,
                doc.getKnowledgeBase() != null ? doc.getKnowledgeBase().getId() : null,
                doc.getKnowledgeBase() != null ? doc.getKnowledgeBase().getName() : null,
                doc.getCompany() != null ? doc.getCompany().getId() : null,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(long chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getKnowledgeBaseName() {
        return knowledgeBaseName;
    }

    public void setKnowledgeBaseName(String knowledgeBaseName) {
        this.knowledgeBaseName = knowledgeBaseName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
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

