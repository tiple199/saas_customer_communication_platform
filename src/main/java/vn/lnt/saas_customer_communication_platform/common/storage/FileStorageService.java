package vn.lnt.saas_customer_communication_platform.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFileInfo storeFile(MultipartFile file, String subDirectory);

    Resource loadFileAsResource(String fileName, String subDirectory);

    String readTextPreview(String fileName, String subDirectory, int maxChars);

    void deleteFile(String fileName, String subDirectory);
}

