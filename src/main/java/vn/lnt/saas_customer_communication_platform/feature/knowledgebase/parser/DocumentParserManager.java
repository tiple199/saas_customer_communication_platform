package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;

import java.io.InputStream;
import java.util.List;

@Service
public class DocumentParserManager {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserManager.class);
    private final List<DocumentParser> parsers;

    public DocumentParserManager(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public String parseDocument(InputStream inputStream, String fileExtension) {
        if (inputStream == null) {
            throw new InvalidOperationException("Luồng dữ liệu tài liệu không được để trống");
        }

        DocumentParser targetParser = parsers.stream()
                .filter(p -> p.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("Không hỗ trợ trích xuất văn bản cho định dạng: ." + fileExtension));

        try {
            String text = targetParser.parse(inputStream);
            return cleanExtractedText(text);
        } catch (Exception ex) {
            log.error("Lỗi khi parse tài liệu định dạng {}: {}", fileExtension, ex.getMessage(), ex);
            throw new InvalidOperationException("Không thể đọc và bóc tách nội dung tài liệu (có thể file bị mã hóa hoặc lỗi cấu trúc): " + ex.getMessage());
        }
    }

    private String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}

