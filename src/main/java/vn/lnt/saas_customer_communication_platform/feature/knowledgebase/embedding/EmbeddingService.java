package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.embedding;

import java.util.List;

public interface EmbeddingService {

    /**
     * Tạo vector embedding cho một đoạn văn bản đơn lẻ.
     */
    List<Float> generateEmbedding(String text);

    /**
     * Tạo vector embedding hàng loạt cho danh sách các phân đoạn văn bản (batch).
     */
    List<List<Float>> generateEmbeddings(List<String> texts);

    /**
     * Tính toán độ tương đồng Cosine giữa 2 vector (kết quả từ -1.0 đến 1.0, càng gần 1.0 càng tương đồng).
     */
    double calculateCosineSimilarity(List<Float> vectorA, List<Float> vectorB);

    /**
     * Chuyển đổi mảng vector Float thành chuỗi JSON để lưu trữ vào CSDL.
     */
    String serializeEmbedding(List<Float> vector);

    /**
     * Đọc chuỗi JSON từ CSDL ra lại mảng vector Float để tính toán similarity.
     */
    List<Float> parseEmbedding(String json);
}

