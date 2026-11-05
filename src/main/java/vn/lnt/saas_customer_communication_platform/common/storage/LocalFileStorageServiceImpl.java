package vn.lnt.saas_customer_communication_platform.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.lnt.saas_customer_communication_platform.exception.FileUploadException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path baseStorageLocation;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "docx", "doc", "txt", "md", "csv", "json"
    );

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

    public LocalFileStorageServiceImpl(@Value("${app.upload.dir:uploads/documents}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (IOException ex) {
            throw new FileUploadException("Không thể khởi tạo thư mục lưu trữ file: " + ex.getMessage());
        }
    }

    @Override
    public StoredFileInfo storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File tải lên không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("Kích thước file vượt quá giới hạn tối đa (25MB)");
        }

        String rawOriginalName = file.getOriginalFilename();
        if (rawOriginalName == null || rawOriginalName.isBlank()) {
            throw new FileUploadException("Tên file không hợp lệ");
        }

        String originalFileName = StringUtils.cleanPath(rawOriginalName);
        if (originalFileName.contains("..")) {
            throw new FileUploadException("Tên file chứa đường dẫn không hợp lệ: " + originalFileName);
        }

        String extension = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileUploadException("Định dạng file ." + extension + " không được hỗ trợ. Chỉ chấp nhận: "
                    + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String storedFileName = UUID.randomUUID().toString() + "." + extension;

        Path targetDir = this.baseStorageLocation;
        if (subDirectory != null && !subDirectory.isBlank()) {
            targetDir = targetDir.resolve(subDirectory).normalize();
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e) {
                throw new FileUploadException("Không thể tạo thư mục lưu trữ con: " + e.getMessage());
            }
        }

        Path targetLocation = targetDir.resolve(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileUploadException("Lỗi khi lưu file '" + originalFileName + "': " + ex.getMessage());
        }

        String relativeUrl = (subDirectory != null && !subDirectory.isBlank())
                ? subDirectory + "/" + storedFileName
                : storedFileName;

        return new StoredFileInfo(
                storedFileName,
                originalFileName,
                extension,
                file.getSize(),
                relativeUrl
        );
    }

    @Override
    public Resource loadFileAsResource(String fileName, String subDirectory) {
        try {
            Path targetDir = this.baseStorageLocation;
            if (subDirectory != null && !subDirectory.isBlank()) {
                targetDir = targetDir.resolve(subDirectory).normalize();
            }
            Path filePath = targetDir.resolve(fileName).normalize();

            // Chống tấn công Directory Traversal
            if (!filePath.startsWith(this.baseStorageLocation)) {
                throw new FileUploadException("Đường dẫn file không hợp lệ");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileUploadException("Không tìm thấy file hoặc file không thể đọc: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileUploadException("Đường dẫn file không hợp lệ: " + ex.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileName, String subDirectory) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Path targetDir = this.baseStorageLocation;
            if (subDirectory != null && !subDirectory.isBlank()) {
                targetDir = targetDir.resolve(subDirectory).normalize();
            }
            Path filePath = targetDir.resolve(fileName).normalize();

            if (filePath.startsWith(this.baseStorageLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
}

