package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.llm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.RagContextResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.llm.LlmService;

import java.util.List;

@Service
public class GeminiLlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmServiceImpl.class);

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final String apiKey;
    private final String chatModel;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiLlmServiceImpl(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.chat-model:gemini-3.1-flash-lite}") String chatModel) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.chatModel = chatModel != null && !chatModel.isBlank() ? chatModel.trim() : "gemini-3.1-flash-lite";
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
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
        return callGeminiGenerateContent(prompt, null);
    }

    private String callGeminiGenerateContent(String prompt, String responseMimeType) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        if (apiKey.isEmpty()) {
            throw new InvalidOperationException("Chưa cấu hình GEMINI_API_KEY. Vui lòng cấu hình API Key trong application.yaml để LLM có thể sinh câu trả lời.");
        }

        ContentItem contentItem = new ContentItem(
                "user",
                List.of(new TextPart(prompt))
        );
        GenerationConfig config = new GenerationConfig(0.2, 1024, responseMimeType);
        GenerateContentRequest requestBody = new GenerateContentRequest(List.of(contentItem), config);

        List<String> candidateModels = new java.util.ArrayList<>();
        candidateModels.add(chatModel);
        for (String fallback : List.of("gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3-flash-preview")) {
            if (!candidateModels.contains(fallback)) {
                candidateModels.add(fallback);
            }
        }

        Exception lastException = null;
        for (String modelName : candidateModels) {
            String url = String.format("/models/%s:generateContent?key=%s", modelName, apiKey);
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
                lastException = ex;
                log.warn("[GeminiLLM] Model '{}' gặp lỗi ({}), đang thử model dự phòng tiếp theo...", modelName, ex.getMessage());
            }
        }

        log.error("[GeminiLLM] Tất cả các model Gemini đều gặp lỗi: {}", lastException != null ? lastException.getMessage() : "Unknown", lastException);
        throw new InvalidOperationException("Lỗi khi kết nối với mô hình AI Gemini: " + (lastException != null ? lastException.getMessage() : ""));
    }

    @Override
    public String generateAnswer(RagContextResponse ragContext) {
        if (ragContext == null) {
            return "";
        }

        String prompt = ragContext.getFullPrompt();
        if (prompt == null || prompt.isBlank()) {
            prompt = ragContext.getQuery();
        }

        // Yêu cầu Gemini trả về Structured JSON Output (application/json) để đánh giá ngữ nghĩa chính xác 100%
        String rawJson = callGeminiGenerateContent(prompt, "application/json");
        if (rawJson == null || rawJson.isBlank()) {
            ragContext.setInsufficientContext(true);
            ragContext.setInsufficientReason("EMPTY_LLM_ANSWER");
            return "";
        }

        try {
            StructuredRagEvaluation eval = objectMapper.readValue(rawJson, StructuredRagEvaluation.class);
            boolean handoff = Boolean.TRUE.equals(eval.humanHandoffRequested());
            boolean llmInsufficient = Boolean.TRUE.equals(eval.insufficientContext()) || handoff;

            ragContext.setHumanHandoffRequested(handoff);
            ragContext.setInsufficientContext(llmInsufficient);

            if (llmInsufficient) {
                String reason = (eval.reason() != null && !eval.reason().isBlank() && !"SUFFICIENT".equalsIgnoreCase(eval.reason()))
                        ? eval.reason()
                        : (handoff ? "HUMAN_HANDOFF_REQUESTED" : "NO_INFORMATION_IN_DOCS");
                ragContext.setInsufficientReason(reason);
            } else {
                ragContext.setInsufficientReason(null);
            }

            return eval.answer() != null ? eval.answer().trim() : "";
        } catch (Exception ex) {
            log.warn("[GeminiLLM] Không thể parse JSON từ Gemini, sử dụng văn bản gốc: {}", ex.getMessage());
            return rawJson;
        }
    }

    // Records chuẩn hóa cho Request, Response và Structured JSON Output của Gemini
    record TextPart(String text) {}
    record ContentItem(String role, List<TextPart> parts) {}
    record GenerationConfig(Double temperature, Integer maxOutputTokens, String responseMimeType) {}
    record GenerateContentRequest(List<ContentItem> contents, GenerationConfig generationConfig) {}

    record CandidateContent(List<TextPart> parts, String role) {}
    record Candidate(CandidateContent content, String finishReason) {}
    record GenerateContentResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StructuredRagEvaluation(
            String answer,
            Boolean insufficientContext,
            Boolean humanHandoffRequested,
            String reason
    ) {}
}

