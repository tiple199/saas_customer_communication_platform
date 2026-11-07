package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.llm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.RagContextResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.llm.LlmService;

import java.util.Collections;
import java.util.List;

@Service
public class GeminiLlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmServiceImpl.class);

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final String apiKey;
    private final String chatModel;
    private final RestClient restClient;

    public GeminiLlmServiceImpl(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.chat-model:gemini-1.5-flash}") String chatModel) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.chatModel = chatModel != null && !chatModel.isBlank() ? chatModel.trim() : "gemini-1.5-flash";
        this.restClient = RestClient.builder()
                .baseUrl(GEMINI_API_BASE)
                .build();

        if (this.apiKey.isEmpty()) {
            log.warn("[GeminiLLM] Chưa cấu hình GEMINI_API_KEY. Chức năng gọi LLM sinh câu trả lời cần có API Key để hoạt động.");
        } else {
            log.info("[GeminiLLM] Đã kích hoạt Google Gemini LLM với model: {}", this.chatModel);
        }
    }

    @Override
    public String generateText(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        if (apiKey.isEmpty()) {
            throw new InvalidOperationException("Chưa cấu hình GEMINI_API_KEY. Vui lòng cấu hình API Key trong application.yaml để LLM có thể sinh câu trả lời.");
        }

        String url = String.format("/models/%s:generateContent?key=%s", chatModel, apiKey);

        // Chuẩn bị Request body theo format của Gemini generateContent
        ContentItem contentItem = new ContentItem(
                "user",
                List.of(new TextPart(prompt))
        );
        GenerationConfig config = new GenerationConfig(0.2, 1024);
        GenerateContentRequest requestBody = new GenerateContentRequest(List.of(contentItem), config);

        try {
            GenerateContentResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GenerateContentResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new InvalidOperationException("Gemini không trả về câu trả lời khả dụng");
            }

            Candidate candidate = response.candidates().get(0);
            if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
                throw new InvalidOperationException("Nội dung phản hồi từ Gemini bị trống");
            }

            StringBuilder answer = new StringBuilder();
            for (TextPart part : candidate.content().parts()) {
                if (part.text() != null) {
                    answer.append(part.text());
                }
            }

            return answer.toString().trim();
        } catch (Exception ex) {
            log.error("[GeminiLLM] Lỗi khi gọi Gemini generateContent: {}", ex.getMessage(), ex);
            throw new InvalidOperationException("Lỗi khi kết nối với mô hình AI Gemini: " + ex.getMessage());
        }
    }

    @Override
    public String generateAnswer(RagContextResponse ragContext) {
        if (ragContext == null) {
            return "";
        }

        // Ưu tiên sử dụng fullPrompt đã được xây dựng từ SCRUM-74
        String prompt = ragContext.getFullPrompt();
        if (prompt == null || prompt.isBlank()) {
            prompt = ragContext.getQuery();
        }

        return generateText(prompt);
    }

    // Records chuẩn hóa cho Request và Response của Gemini generateContent
    record TextPart(String text) {}
    record ContentItem(String role, List<TextPart> parts) {}
    record GenerationConfig(Double temperature, Integer maxOutputTokens) {}
    record GenerateContentRequest(List<ContentItem> contents, GenerationConfig generationConfig) {}

    record CandidateContent(List<TextPart> parts, String role) {}
    record Candidate(CandidateContent content, String finishReason) {}
    record GenerateContentResponse(List<Candidate> candidates) {}
}

