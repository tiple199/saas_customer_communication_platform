package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.impl;

import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser.DocumentParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class TextDocumentParser implements DocumentParser {

    private static final List<String> SUPPORTED = Arrays.asList("txt", "md", "csv", "json");

    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null) return false;
        return SUPPORTED.contains(fileExtension.toLowerCase().trim());
    }

    @Override
    public String parse(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

