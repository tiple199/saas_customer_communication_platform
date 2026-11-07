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

        // 3. Xây dựng khối Lịch sử hội thoại gần đây (nếu có truyền conversationId)
        String conversationHistoryContext = formatConversationHistory(companyId, conversationId);

        // 4. Xây dựng System Instruction chuẩn cho AI của công ty
        String systemInstruction = buildSystemInstruction(company.getName());

        // 5. Ghép nối thành Full Augmented Prompt hoàn chỉnh để gửi cho LLM
        String fullPrompt = assembleFullPrompt(
                systemInstruction,
                knowledgeContext,
                conversationHistoryContext,
                query.trim(),
                hasRelevantKnowledge
        );

        RagContextResponse response = new RagContextResponse();
        response.setCompanyId(companyId);
        response.setKnowledgeBaseId(knowledgeBaseId);
        response.setConversationId(conversationId);
        response.setQuery(query.trim());
        response.setHasRelevantKnowledge(hasRelevantKnowledge);
        response.setMatchedChunks(matchedChunks);
        response.setKnowledgeContext(knowledgeContext);
        response.setConversationHistoryContext(conversationHistoryContext);
        response.setSystemInstruction(systemInstruction);
        response.setFullPrompt(fullPrompt);

        return response;
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

    private String formatConversationHistory(Long companyId, Long conversationId) {
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

        int startIdx = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        List<ConversationMessage> recentMessages = messages.subList(startIdx, messages.size());

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
                1. Chỉ sử dụng thông tin có trong phần [DỮ LIỆU CƠ SỞ TRI THỨC] bên dưới để trả lời câu hỏi của khách hàng.
                2. Trả lời bằng tiếng Việt lịch sự, thân thiện, rõ ràng, ngắn gọn và đi thẳng vào trọng tâm.
                3. Nếu thông tin không có trong [DỮ LIỆU CƠ SỞ TRI THỨC], hãy lịch sự thông báo rằng bạn chưa có thông tin chi tiết về vấn đề này và đề xuất chuyển cuộc trò chuyện cho nhân viên tư vấn hỗ trợ. Tuyệt đối KHÔNG tự ý bịa đặt thông tin, giá cả hoặc chính sách ngoài tài liệu.
                """,
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
              .append("Hãy đưa ra câu trả lời phù hợp nhất dành cho khách hàng:");

        return prompt.toString();
    }
}

