package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationMessageRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.DocumentChunkSearchResult;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.RagContextResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.DocumentService;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.RagContextService;

import java.util.*;

@Service
public class RagContextServiceImpl implements RagContextService {

    private static final int MAX_KNOWLEDGE_CHARS = 6000;
    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final double CONFIDENT_SIMILARITY_THRESHOLD = 0.45;
    public static final String INSUFFICIENT_CONTEXT_TAG = "[INSUFFICIENT_CONTEXT]";

    private final DocumentService documentService;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    public RagContextServiceImpl(DocumentService documentService,
                                 CompanyRepository companyRepository,
                                 CompanyMemberRepository companyMemberRepository,
                                 UserRepository userRepository,
                                 ConversationRepository conversationRepository,
                                 ConversationMessageRepository conversationMessageRepository) {
        this.documentService = documentService;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RagContextResponse buildContext(String currentUserEmail, Long companyId, Long knowledgeBaseId,
                                           Long conversationId, String query, int topK, double minScore) {
        if (currentUserEmail == null || currentUserEmail.isBlank()) {
            throw new InvalidOperationException("Không xác định được người dùng hiện tại");
        }
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Thiếu thông tin companyId");
        }

        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), targetCompanyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));
        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản thành viên của bạn chưa kích hoạt");
        }

        return buildContextForCompany(targetCompanyId, knowledgeBaseId, conversationId, query, topK, minScore);
    }

    @Override
    @Transactional(readOnly = true)
    public RagContextResponse buildContextForCompany(Long companyId, Long knowledgeBaseId,
                                                     Long conversationId, String query, int topK, double minScore) {
        if (companyId == null) {
            throw new InvalidOperationException("Thiếu thông tin companyId khi xây dựng context RAG");
        }
        if (query == null || query.isBlank()) {
            throw new InvalidOperationException("Câu hỏi truy vấn không được để trống");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        // 1. Truy xuất các phân đoạn tài liệu liên quan nhất (Semantic Search + Filter theo companyId)
        int effectiveTopK = topK > 0 ? topK : 5;
        double effectiveMinScore = minScore > 0 ? minScore : 0.35;

        List<DocumentChunkSearchResult> matchedChunks = documentService.retrieveSimilarChunksByCompany(
                companyId, knowledgeBaseId, query.trim(), effectiveTopK, effectiveMinScore
        );

        // 2. Xây dựng khối văn bản Tri thức (Knowledge Context) có kiểm soát độ dài và chống trùng lặp
        String knowledgeContext = formatKnowledgeContext(matchedChunks);
        boolean hasRelevantKnowledge = !matchedChunks.isEmpty() && !knowledgeContext.isBlank();

        double maxSimilarityScore = matchedChunks.stream()
                .map(DocumentChunkSearchResult::getSimilarityScore)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        String trimmedQuery = query.trim();

        // Xác định Pre-LLM dựa trên toán học Vector Similarity:
        // Nếu không tìm thấy chunk nào hoặc điểm tương đồng cao nhất < ngưỡng tin cậy (0.45)
        boolean insufficientContext = !hasRelevantKnowledge || maxSimilarityScore < CONFIDENT_SIMILARITY_THRESHOLD;
        String insufficientReason = !hasRelevantKnowledge
                ? "NO_MATCHED_CHUNKS"
                : (maxSimilarityScore < CONFIDENT_SIMILARITY_THRESHOLD ? "LOW_SIMILARITY_SCORE" : null);

        // 3. Xây dựng khối Lịch sử hội thoại gần đây (nếu có truyền conversationId)
        String conversationHistoryContext = formatConversationHistory(companyId, conversationId, trimmedQuery);

        // 4. Xây dựng System Instruction chuẩn cho AI của công ty (Structured JSON Output)
        String systemInstruction = buildSystemInstruction(company.getName());

        // 5. Ghép nối thành Full Augmented Prompt hoàn chỉnh để gửi cho LLM
        String fullPrompt = assembleFullPrompt(
                systemInstruction,
                knowledgeContext,
                conversationHistoryContext,
                trimmedQuery,
                hasRelevantKnowledge
        );

        RagContextResponse response = new RagContextResponse();
        response.setCompanyId(companyId);
        response.setKnowledgeBaseId(knowledgeBaseId);
        response.setConversationId(conversationId);
        response.setQuery(trimmedQuery);
        response.setHasRelevantKnowledge(hasRelevantKnowledge);
        response.setMaxSimilarityScore(maxSimilarityScore);
        response.setInsufficientContext(insufficientContext);
        response.setHumanHandoffRequested(false);
        response.setInsufficientReason(insufficientReason);
        response.setMatchedChunks(matchedChunks);
        response.setKnowledgeContext(knowledgeContext);
        response.setConversationHistoryContext(conversationHistoryContext);
        response.setSystemInstruction(systemInstruction);
        response.setFullPrompt(fullPrompt);

        return response;
    }

    @Override
    public boolean isInsufficientContext(RagContextResponse context, String aiAnswer) {
        if (context == null) {
            return true;
        }
        if (aiAnswer == null || aiAnswer.isBlank()) {
            context.setInsufficientContext(true);
            context.setInsufficientReason("EMPTY_LLM_ANSWER");
            return true;
        }
        // Kết quả được quyết định bởi Structured JSON Output của LLM (hiểu ngữ nghĩa câu hỏi & tài liệu)
        // kết hợp với đánh giá Vector Similarity trong context
        return context.isInsufficientContext() || context.isHumanHandoffRequested();
    }

    private String formatKnowledgeContext(List<DocumentChunkSearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Set<String> seenContents = new HashSet<>();
        int index = 1;

        for (DocumentChunkSearchResult chunk : chunks) {
            if (chunk.getContent() == null || chunk.getContent().isBlank()) {
                continue;
            }
            String normalized = chunk.getContent().trim();
            if (!seenContents.add(normalized)) {
                continue; // Bỏ qua nếu đoạn văn bản bị trùng lặp
            }

            double scorePercent = Math.round((chunk.getSimilarityScore() != null ? chunk.getSimilarityScore() : 0.0) * 1000.0) / 10.0;
            String header = String.format(
                    "[Nguồn #%d | Tài liệu: %s | Đoạn #%d | Độ tương đồng: %.1f%%]\n",
                    index,
                    chunk.getDocumentName() != null ? chunk.getDocumentName() : "Không rõ",
                    (chunk.getChunkIndex() != null ? chunk.getChunkIndex() + 1 : index),
                    scorePercent
            );

            if (sb.length() + header.length() + normalized.length() > MAX_KNOWLEDGE_CHARS) {
                int remaining = MAX_KNOWLEDGE_CHARS - sb.length() - header.length();
                if (remaining > 150) {
                    sb.append(header).append(normalized, 0, remaining).append("...\n\n");
                }
                break;
            }

            sb.append(header).append(normalized).append("\n\n");
            index++;
        }

        return sb.toString().trim();
    }

    private String formatConversationHistory(Long companyId, Long conversationId, String currentQuery) {
        if (conversationId == null) {
            return "";
        }

        Optional<Conversation> convOpt = conversationRepository.findByIdAndChannelCompanyId(conversationId, companyId);
        if (convOpt.isEmpty()) {
            return "";
        }

        List<ConversationMessage> messages = conversationMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        // Loại bỏ tin nhắn vừa mới gửi ở cuối lịch sử để tránh lặp lại với phần [CÂU HỎI HIỆN TẠI]
        int endIdx = messages.size();
        if (endIdx > 0 && currentQuery != null && !currentQuery.isBlank()) {
            ConversationMessage lastMsg = messages.get(endIdx - 1);
            if ("VISITOR".equalsIgnoreCase(lastMsg.getSenderType())
                    && lastMsg.getContent() != null
                    && lastMsg.getContent().trim().equalsIgnoreCase(currentQuery.trim())) {
                endIdx--;
            }
        }
        if (endIdx <= 0) {
            return "";
        }

        int startIdx = Math.max(0, endIdx - MAX_HISTORY_MESSAGES);
        List<ConversationMessage> recentMessages = messages.subList(startIdx, endIdx);

        StringBuilder sb = new StringBuilder();
        for (ConversationMessage msg : recentMessages) {
            if (msg.getContent() == null || msg.getContent().isBlank()) {
                continue;
            }
            String roleLabel = resolveSenderLabel(msg.getSenderType());
            sb.append(roleLabel).append(": ").append(msg.getContent().trim()).append("\n");
        }

        return sb.toString().trim();
    }

    private String resolveSenderLabel(String senderType) {
        if (senderType == null) {
            return "Người dùng";
        }
        return switch (senderType.toUpperCase()) {
            case "VISITOR", "CUSTOMER" -> "Khách hàng";
            case "AI", "BOT" -> "Trợ lý AI";
            case "AGENT", "STAFF", "USER", "ADMIN" -> "Nhân viên hỗ trợ";
            default -> senderType;
        };
    }

    private String buildSystemInstruction(String companyName) {
        String safeCompanyName = (companyName != null && !companyName.isBlank()) ? companyName : "chúng tôi";
        return String.format(
                """
                Bạn là Trợ lý AI chăm sóc khách hàng chính thức của %s.
                Nguyên tắc phản hồi bắt buộc:
                1. Nếu khách hàng chào hỏi xã giao, cảm ơn hoặc trò chuyện lịch sự thông thường: hãy phản hồi thân thiện, tự nhiên với tư cách Trợ lý AI của %s và đặt "insufficientContext": false, "humanHandoffRequested": false, "reason": "SUFFICIENT".
                2. Nếu khách hàng bày tỏ ý muốn gặp nhân viên thật, tư vấn viên, quản lý, hoặc không muốn nói chuyện với AI (dù diễn đạt bằng bất kỳ cách nào): hãy lịch sự thông báo sẽ chuyển cuộc trò chuyện cho nhân viên hỗ trợ ngay và đặt "insufficientContext": true, "humanHandoffRequested": true, "reason": "HUMAN_HANDOFF_REQUESTED".
                3. Đối với mọi câu hỏi về sản phẩm, dịch vụ, bảng giá, quy định hoặc chính sách: CHỈ sử dụng thông tin có trong [DỮ LIỆU CƠ SỞ TRI THỨC] để trả lời. Nếu có đủ thông tin trả lời, đặt "insufficientContext": false, "humanHandoffRequested": false, "reason": "SUFFICIENT".
                4. Nếu [DỮ LIỆU CƠ SỞ TRI THỨC] KHÔNG có thông tin hoặc không đủ dữ kiện để trả lời chính xác câu hỏi của khách hàng: TUYỆT ĐỐI KHÔNG tự bịa đặt thông tin, hãy lịch sự xin lỗi vì chưa có thông tin chi tiết, đề xuất chuyển cho nhân viên tư vấn và đặt "insufficientContext": true, "humanHandoffRequested": false, "reason": "NO_INFORMATION_IN_DOCS".
                5. BẮT BUỘC trả về kết quả dưới dạng một đối tượng JSON hợp lệ theo đúng cấu trúc sau:
                {
                  "answer": "Nội dung câu trả lời tiếng Việt lịch sự gửi trực tiếp cho khách hàng",
                  "insufficientContext": true hoặc false,
                  "humanHandoffRequested": true hoặc false,
                  "reason": "SUFFICIENT | NO_INFORMATION_IN_DOCS | HUMAN_HANDOFF_REQUESTED"
                }
                """,
                safeCompanyName,
                safeCompanyName
        ).trim();
    }

    private String assembleFullPrompt(String systemInstruction,
                                      String knowledgeContext,
                                      String conversationHistoryContext,
                                      String userQuery,
                                      boolean hasRelevantKnowledge) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("=== CHỈ DẪN HỆ THỐNG ===\n")
              .append(systemInstruction)
              .append("\n\n");

        prompt.append("=== DỮ LIỆU CƠ SỞ TRI THỨC ===\n");
        if (hasRelevantKnowledge) {
            prompt.append(knowledgeContext).append("\n\n");
        } else {
            prompt.append("(Không tìm thấy phân đoạn tài liệu nào khớp với câu hỏi trong cơ sở tri thức)\n\n");
        }

        if (conversationHistoryContext != null && !conversationHistoryContext.isBlank()) {
            prompt.append("=== LỊCH SỬ HỘI THOẠI GẦN ĐÂY ===\n")
                  .append(conversationHistoryContext)
                  .append("\n\n");
        }

        prompt.append("=== CÂU HỎI HIỆN TẠI CỦA KHÁCH HÀNG ===\n")
              .append(userQuery)
              .append("\n\n")
              .append("Hãy phân tích ngữ nghĩa câu hỏi và trả về đúng JSON theo cấu trúc đã quy định:");

        return prompt.toString();
    }
}

