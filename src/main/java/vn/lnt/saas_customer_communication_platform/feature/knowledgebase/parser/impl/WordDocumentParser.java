package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.impl;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.DocumentParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
public class WordDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null) return false;
        String ext = fileExtension.toLowerCase().trim();
        return ext.equals("docx") || ext.equals("doc");
    }

    @Override
    public String parse(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            // Thử parse DOCX
            try (XWPFDocument docx = new XWPFDocument(bais);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
                return extractor.getText();
            } catch (Exception exDocx) {
                // Nếu không phải DOCX, thử parse DOC (Word 97-2003)
                try (ByteArrayInputStream baisDoc = new ByteArrayInputStream(bytes);
                     HWPFDocument doc = new HWPFDocument(baisDoc);
                     WordExtractor extractorDoc = new WordExtractor(doc)) {
                    return extractorDoc.getText();
                }
            }
        }
    }
}

