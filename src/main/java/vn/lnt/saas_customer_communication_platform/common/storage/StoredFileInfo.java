package vn.lnt.saas_customer_communication_platform.common.storage;

public class StoredFileInfo {

    private String storedFileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String relativeUrl;

    public StoredFileInfo() {
    }

    public StoredFileInfo(String storedFileName, String originalFileName, String fileType, Long fileSize, String relativeUrl) {
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.relativeUrl = relativeUrl;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
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

    public String getRelativeUrl() {
        return relativeUrl;
    }

    public void setRelativeUrl(String relativeUrl) {
        this.relativeUrl = relativeUrl;
    }
}

