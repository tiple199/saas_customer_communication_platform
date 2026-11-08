package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.embedding.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.embedding.EmbeddingService;

import java.util.*;

@Service
public class GeminiEmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingServiceImpl.class);

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final int BATCH_SIZE = 50; // Gemini hỗ trợ batch lên tới 100 texts mỗi request

    private final String apiKey;
    private final String embeddingModel;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingServiceImpl(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.embedding-model:gemini-embedding-001}") String embeddingModel) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.embeddingModel = embeddingModel != null && !embeddingModel.isBlank() ? embeddingModel.trim() : "gemini-embedding-001";
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(GEMINI_API_BASE)
                .build();

        if (this.apiKey.isEmpty()) {
            log.warn("[GeminiEmbedding] Chưa cấu hình GEMINI_API_KEY trong application.yaml. Cần cấu hình key để thực hiện tạo Vector Embedding và Semantic Search.");
        } else {
            log.info("[GeminiEmbedding] Đã kích hoạt Google Gemini Embedding với model: {}", this.embeddingModel);
        }
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<List<Float>> results = generateEmbeddings(Collections.singletonList(text));
        return results.isEmpty() ? Collections.emptyList() : results.get(0);
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        // Bắt buộc phải có API Key thật để đảm bảo hiểu đúng ngữ nghĩa RAG
        if (apiKey.isEmpty()) {
            throw new InvalidOperationException("Chưa cấu hình GEMINI_API_KEY. Vui lòng cấu hình API Key trong application.yaml hoặc biến môi trường GEMINI_API_KEY để hệ thống có thể tạo Vector Embedding cho tài liệu.");
        }

        List<List<Float>> allEmbeddings = new ArrayList<>();

        // Chia thành từng batch nhỏ hơn hoặc bằng BATCH_SIZE để gửi cho Gemini
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);
            try {
                List<List<Float>> batchEmbeddings = callGeminiBatchEmbed(batch);
                allEmbeddings.addAll(batchEmbeddings);
            } catch (Exception e) {
                log.error("[GeminiEmbedding] Lỗi khi gọi Gemini batchEmbedContents API: {}", e.getMessage(), e);
                throw new InvalidOperationException("Lỗi khi gọi Google Gemini Embedding API: " + e.getMessage());
            }
        }

        return allEmbeddings;
    }

    private List<List<Float>> callGeminiBatchEmbed(List<String> batchTexts) {
        String modelName = "models/" + embeddingModel;
        String url = String.format("/models/%s:batchEmbedContents?key=%s", embeddingModel, apiKey);

        // 1. Đóng gói danh sách văn bản thành các request theo chuẩn Gemini
        List<EmbedRequest> requests = batchTexts.stream()
                .map(text -> new EmbedRequest(modelName, new Content(List.of(new TextPart(text)))))
                .toList();

        // 2. Gửi request và Spring RestClient tự động map JSON trả về thành BatchEmbedResponse
        BatchEmbedResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BatchEmbedRequest(requests))
                .retrieve()
                .body(BatchEmbedResponse.class);

        if (response == null || response.embeddings() == null) {
            return Collections.emptyList();
        }

        // 3. Trích xuất danh sách các vector 768 chiều trả về
        return response.embeddings().stream()
                .map(EmbeddingItem::values)
                .toList();
    }

    // Các DTO record chuẩn hóa cấu trúc Request/Response của Google Gemini API
    record TextPart(String text) {}
    record Content(List<TextPart> parts) {}
    record EmbedRequest(String model, Content content) {}
    record BatchEmbedRequest(List<EmbedRequest> requests) {}

    record EmbeddingItem(List<Float> values) {}
    record BatchEmbedResponse(List<EmbeddingItem> embeddings) {}

    @Override
    public double calculateCosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }
        int dim = Math.min(vectorA.size(), vectorB.size());
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < dim; i++) {
            float valA = vectorA.get(i);
            float valB = vectorB.get(i);
            dotProduct += valA * valB;
            normA += valA * valA;
            normB += valB * valB;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public String serializeEmbedding(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            log.error("[GeminiEmbedding] Lỗi khi serialize vector: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<Float> parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Float>>() {});
        } catch (Exception e) {
            log.error("[GeminiEmbedding] Lỗi khi parse vector JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
