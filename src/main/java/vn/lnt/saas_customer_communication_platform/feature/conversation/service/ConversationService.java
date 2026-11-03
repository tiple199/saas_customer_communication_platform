package vn.lnt.saas_customer_communication_platform.feature.conversation.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.CreateConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;

import java.util.List;

public interface ConversationService {

    /**
     * Tạo mới một Conversation cho Visitor trên Channel chỉ định.
     * Tự động thiết lập handlingMode = AI nếu Channel bật AI, ngược lại là HUMAN.
     */
    Conversation createConversation(Channel channel, Visitor visitor);

    /**
     * Tìm Conversation đang hoạt động (OPEN) của Visitor trên Channel,
     * nếu chưa có hoặc đã đóng/giải quyết thì tạo mới Conversation.
     */
    ConversationResolutionResult getOrCreateActiveConversation(Channel channel, Visitor visitor);

    /**
     * Khôi phục cuộc hội thoại gần nhất của Visitor trên Channel kèm lịch sử tin nhắn.
     */
    RestoreConversationResponse restoreVisitorConversation(Channel channel, Visitor visitor);

    /**
     * Khách hàng (Visitor) gửi tin nhắn vào cuộc hội thoại trên Channel.
     * Nếu cuộc hội thoại đang RESOLVED thì tự động mở lại thành OPEN; nếu chưa có hoặc đã CLOSED thì tạo mới.
     */
    ConversationMessageResponse sendVisitorMessage(Channel channel, Visitor visitor, String content, String messageType);

    /**
     * Mở lại (Restore/Reopen) một cuộc hội thoại theo ID về trạng thái OPEN kèm lịch sử tin nhắn (dành cho Agent).
     */
    RestoreConversationResponse restoreConversationById(String currentUserEmail, Long companyId, Long conversationId);

    /**
     * Lấy danh sách lịch sử tin nhắn của một Conversation (dành cho thành viên công ty).
     */
    List<ConversationMessageResponse> getMessagesByConversationId(String currentUserEmail, Long companyId, Long conversationId);

    /**
     * Tạo hoặc lấy Conversation đang hoạt động cho Visitor & Channel thuộc công ty (dành cho thành viên công ty).
     */
    ConversationResponse createConversationForCompany(String currentUserEmail, Long companyId, CreateConversationRequest request);

    /**
     * Lấy chi tiết Conversation theo ID (cô lập theo companyId).
     */
    ConversationResponse getConversationById(String currentUserEmail, Long companyId, Long conversationId);

    /**
     * Lấy danh sách tất cả Conversation của công ty.
     */
    List<ConversationResponse> getConversations(String currentUserEmail, Long companyId);

    /**
     * Lấy danh sách Conversation của công ty có phân trang và lọc theo trạng thái.
     */
    PageResponse<ConversationResponse> getConversationsPaged(String currentUserEmail, Long companyId,
                                                            ConversationStatus status, Pageable pageable);
}

