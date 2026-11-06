package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser;

import java.io.InputStream;

public interface DocumentParser {

    boolean supports(String fileExtension);

    String parse(InputStream inputStream) throws Exception;
}

