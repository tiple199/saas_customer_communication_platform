package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.DocumentParser;

import java.io.InputStream;

@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return fileExtension != null && fileExtension.equalsIgnoreCase("pdf");
    }

    @Override
    public String parse(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
}

