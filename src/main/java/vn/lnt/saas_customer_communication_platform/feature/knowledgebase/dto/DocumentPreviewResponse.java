package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto;

import java.time.LocalDateTime;

public class DocumentPreviewResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private long chunkCount;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long companyId;
    private LocalDateTime createdAt;
    private String textContent;
    private boolean isText;
    private boolean isPdf;
    private boolean isOffice;
    private boolean truncated;

    public DocumentPreviewResponse() {
    }

    public DocumentPreviewResponse(Long id, String fileName, String fileType, Long fileSize, String status,
                                   long chunkCount, Long knowledgeBaseId, String knowledgeBaseName,
                                   Long companyId, LocalDateTime createdAt, String textContent,
                                   boolean isText, boolean isPdf, boolean isOffice, boolean truncated) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.chunkCount = chunkCount;
        this.knowledgeBaseId = knowledgeBaseId;
        this.knowledgeBaseName = knowledgeBaseName;
        this.companyId = companyId;
        this.createdAt = createdAt;
        this.textContent = textContent;
        this.isText = isText;
        this.isPdf = isPdf;
        this.isOffice = isOffice;
        this.truncated = truncated;
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

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public boolean isText() {
        return isText;
    }

    public void setText(boolean text) {
        isText = text;
    }

    public boolean isPdf() {
        return isPdf;
    }

    public void setPdf(boolean pdf) {
        isPdf = pdf;
    }

    public boolean isOffice() {
        return isOffice;
    }

    public void setOffice(boolean office) {
        isOffice = office;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }
}

