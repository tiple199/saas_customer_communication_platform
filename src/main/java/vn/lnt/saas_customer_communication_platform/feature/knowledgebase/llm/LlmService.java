package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.llm;

import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.RagContextResponse;

public interface LlmService {

    /**
     * Gửi prompt trực tiếp lên mô hình ngôn ngữ lớn (LLM) và nhận kết quả văn bản.
     */
    String generateText(String prompt);

    /**
     * Đọc toàn bộ ngữ cảnh RAG (Context) đã chuẩn bị và sinh câu trả lời hoàn chỉnh cho khách hàng.
     */
    String generateAnswer(RagContextResponse ragContext);
}

