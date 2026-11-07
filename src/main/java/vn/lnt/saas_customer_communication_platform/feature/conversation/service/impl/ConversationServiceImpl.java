package vn.lnt.saas_customer_communication_platform.feature.conversation.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.CreateConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.SendConversationMessageRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.HandlingMode;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationMessageRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.VisitorRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.ConversationService;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto.RagContextResponse;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.llm.LlmService;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.service.RagContextService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ChannelRepository channelRepository;
    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RagContextService ragContextService;
    private final LlmService llmService;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   ConversationMessageRepository conversationMessageRepository,
                                   ChannelRepository channelRepository,
                                   VisitorRepository visitorRepository,
                                   UserRepository userRepository,
                                   CompanyMemberRepository companyMemberRepository,
                                   SimpMessagingTemplate messagingTemplate,
                                   RagContextService ragContextService,
                                   LlmService llmService) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.channelRepository = channelRepository;
        this.visitorRepository = visitorRepository;
        this.userRepository = userRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.messagingTemplate = messagingTemplate;
        this.ragContextService = ragContextService;
        this.llmService = llmService;
    }

    @Override
    @Transactional
    public Conversation createConversation(Channel channel, Visitor visitor) {
        validateChannelAndVisitor(channel, visitor);

        Conversation conversation = new Conversation();
        conversation.setChannel(channel);
        conversation.setVisitor(visitor);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setHandlingMode(
                Boolean.TRUE.equals(channel.getAiEnabled()) ? HandlingMode.AI : HandlingMode.HUMAN
        );

        Conversation saved = conversationRepository.save(conversation);
        broadcastConversationUpdate(saved);
        return saved;
    }

    @Override
    @Transactional
    public ConversationResolutionResult getOrCreateActiveConversation(Channel channel, Visitor visitor) {
        validateChannelAndVisitor(channel, visitor);

        Optional<Conversation> activeOpt = conversationRepository
                .findFirstByVisitorIdAndChannelIdAndStatusOrderByIdDesc(
                        visitor.getId(),
                        channel.getId(),
                        ConversationStatus.OPEN
                );

        if (activeOpt.isPresent()) {
            return new ConversationResolutionResult(activeOpt.get(), false);
        }

        Conversation created = createConversation(channel, visitor);
        return new ConversationResolutionResult(created, true);
    }

    @Override
    @Transactional
    public RestoreConversationResponse restoreVisitorConversation(Channel channel, Visitor visitor) {
        validateChannelAndVisitor(channel, visitor);

        Optional<Conversation> latestOpt = conversationRepository
                .findFirstByVisitorIdAndChannelIdOrderByIdDesc(visitor.getId(), channel.getId());

        if (latestOpt.isEmpty()) {
            return new RestoreConversationResponse(null, List.of());
        }

        Conversation conversation = latestOpt.get();
        // Nếu cuộc hội thoại cũ đã CLOSED hẳn thì không khôi phục nữa
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            return new RestoreConversationResponse(null, List.of());
        }

        // Nếu cuộc hội thoại ở trạng thái RESOLVED, mở lại thành OPEN khi khách khôi phục phiên
        if (conversation.getStatus() == ConversationStatus.RESOLVED) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setResolvedAt(null);
            conversation = conversationRepository.save(conversation);
        }

        List<ConversationMessageResponse> messages = getConversationMessages(conversation.getId());
        return new RestoreConversationResponse(ConversationResponse.fromEntity(conversation), messages);
    }

    @Override
    @Transactional
    public ConversationMessageResponse sendVisitorMessage(Channel channel, Visitor visitor, String content, String messageType) {
        validateChannelAndVisitor(channel, visitor);
        if (content == null || content.isBlank()) {
            throw new InvalidOperationException("Nội dung tin nhắn không được để trống");
        }

        Optional<Conversation> latestOpt = conversationRepository
                .findFirstByVisitorIdAndChannelIdOrderByIdDesc(visitor.getId(), channel.getId());

        Conversation conversation;
        if (latestOpt.isEmpty() || latestOpt.get().getStatus() == ConversationStatus.CLOSED) {
            conversation = createConversation(channel, visitor);
        } else {
            conversation = latestOpt.get();
            if (conversation.getStatus() != ConversationStatus.OPEN) {
                conversation.setStatus(ConversationStatus.OPEN);
                conversation.setResolvedAt(null);
            }
            conversation.setUpdatedAt(LocalDateTime.now());
            conversation = conversationRepository.save(conversation);
        }

        ConversationMessageResponse visitorMsgResponse = saveConversationMessage(
                channel.getCompany(),
                conversation,
                "VISITOR",
                visitor.getId(),
                content,
                messageType
        );

        // Kích hoạt AI RAG Auto-Reply nếu Channel bật AI và cuộc hội thoại đang ở chế độ HandlingMode.AI (SCRUM-75, 76, 77)
        if (Boolean.TRUE.equals(channel.getAiEnabled()) && conversation.getHandlingMode() == HandlingMode.AI) {
            triggerAiAutoReply(channel.getCompany(), conversation, content);
        }

        return visitorMsgResponse;
    }

    private void triggerAiAutoReply(Company company, Conversation conversation, String userQuery) {
        try {
            // 1. SCRUM-74: Xây dựng Context RAG từ câu hỏi của khách hàng
            RagContextResponse context = ragContextService.buildContextForCompany(
                    company.getId(),
                    null, // Tự động tìm kiếm trên toàn bộ tài liệu READY của công ty
                    conversation.getId(),
                    userQuery,
                    5,
                    0.35
            );

            // 2. SCRUM-75 & SCRUM-76: Gọi LLM (Gemini 1.5 Flash) để sinh câu trả lời
            String aiAnswer = llmService.generateAnswer(context);

            // 3. SCRUM-77: Lưu câu trả lời của AI vào ConversationMessage (senderType = "AI") và phát realtime WebSocket
            if (aiAnswer != null && !aiAnswer.isBlank()) {
                saveConversationMessage(
                        company,
                        conversation,
                        "AI",
                        null,
                        aiAnswer,
                        "TEXT"
                );
            }
        } catch (Exception ex) {
            log.error("[RAG AutoReply] Lỗi khi AI tự động phản hồi cuộc hội thoại {}: {}", conversation.getId(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ConversationMessageResponse sendStaffMessage(String currentUserEmail, Long companyId, Long conversationId,
                                                        SendConversationMessageRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new InvalidOperationException("Nội dung tin nhắn không được để trống");
        }

        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Conversation conversation = conversationRepository.findByIdAndChannelCompanyId(conversationId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new InvalidOperationException("Cuộc hội thoại đã đóng hoàn toàn, không thể gửi tin nhắn");
        }

        if (conversation.getStatus() != ConversationStatus.OPEN) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setResolvedAt(null);
        }

        // Tự động gán nhân viên đang trả lời nếu cuộc hội thoại chưa có người phụ trách và chuyển sang HUMAN
        if (conversation.getAssigneeId() == null) {
            conversation.setAssigneeId(currentUser.getId());
        }
        conversation.setHandlingMode(HandlingMode.HUMAN);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        Company company = conversation.getChannel() != null ? conversation.getChannel().getCompany() : null;
        return saveConversationMessage(
                company,
                conversation,
                "STAFF",
                currentUser.getId(),
                request.getContent(),
                request.getMessageType()
        );
    }

    private ConversationMessageResponse saveConversationMessage(Company company, Conversation conversation,
                                                                String senderType, Long senderId,
                                                                String content, String messageType) {
        String normalizedType = (messageType == null || messageType.isBlank())
                ? "TEXT"
                : messageType.trim().toUpperCase();

        ConversationMessage message = new ConversationMessage();
        message.setCompany(company);
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(content.trim());
        message.setMessageType(normalizedType);

        ConversationMessage savedMessage = conversationMessageRepository.save(message);
        ConversationMessageResponse response = ConversationMessageResponse.fromEntity(savedMessage);

        // Phát realtime tin nhắn tới cả Customer (Widget) và Staff đang mở cuộc hội thoại này
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), response);
        } catch (Exception ignored) {
        }

        // Phát realtime cập nhật cuộc hội thoại tới danh sách Hộp thư của Staff trong công ty
        broadcastConversationUpdate(conversation);

        return response;
    }

    private void broadcastConversationUpdate(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        try {
            ConversationResponse convResp = ConversationResponse.fromEntity(conversation);
            if (convResp.getCompanyId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/companies/" + convResp.getCompanyId() + "/conversations",
                        convResp
                );
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional
    public RestoreConversationResponse restoreConversationById(String currentUserEmail, Long companyId, Long conversationId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Conversation conversation = conversationRepository.findByIdAndChannelCompanyId(conversationId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (conversation.getStatus() != ConversationStatus.OPEN) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setResolvedAt(null);
            conversation = conversationRepository.save(conversation);
            broadcastConversationUpdate(conversation);
        }

        List<ConversationMessageResponse> messages = getConversationMessages(conversation.getId());
        return new RestoreConversationResponse(ConversationResponse.fromEntity(conversation), messages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> getMessagesByConversationId(String currentUserEmail, Long companyId, Long conversationId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Conversation conversation = conversationRepository.findByIdAndChannelCompanyId(conversationId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        return getConversationMessages(conversation.getId());
    }

    @Override
    @Transactional
    public ConversationResponse assignStaff(String currentUserEmail, Long companyId, Long conversationId, Long assigneeId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Conversation conversation = conversationRepository.findByIdAndChannelCompanyId(conversationId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        Long targetAssigneeId = (assigneeId != null) ? assigneeId : currentUser.getId();
        validateActiveCompanyMember(targetAssigneeId, targetCompanyId);

        conversation.setAssigneeId(targetAssigneeId);
        conversation.setHandlingMode(HandlingMode.HUMAN);
        if (conversation.getStatus() == ConversationStatus.PENDING) {
            conversation.setStatus(ConversationStatus.OPEN);
        }

        Conversation saved = conversationRepository.save(conversation);
        broadcastConversationUpdate(saved);
        return ConversationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ConversationResponse updateConversationStatus(String currentUserEmail, Long companyId,
                                                         Long conversationId, ConversationStatus status) {
        if (status == null) {
            throw new InvalidOperationException("Trạng thái cuộc hội thoại không được để trống");
        }

        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Conversation conversation = conversationRepository.findByIdAndChannelCompanyId(conversationId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        conversation.setStatus(status);
        if (status == ConversationStatus.RESOLVED || status == ConversationStatus.CLOSED) {
            if (conversation.getResolvedAt() == null) {
                conversation.setResolvedAt(LocalDateTime.now());
            }
        } else {
            conversation.setResolvedAt(null);
        }

        Conversation saved = conversationRepository.save(conversation);
        broadcastConversationUpdate(saved);
        return ConversationResponse.fromEntity(saved);
    }

    private List<ConversationMessageResponse> getConversationMessages(Long conversationId) {
        return conversationMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(ConversationMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationResponse createConversationForCompany(String currentUserEmail, Long companyId, CreateConversationRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Channel channel = channelRepository.findByIdAndCompanyId(request.getChannelId(), targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", request.getChannelId()));

        if (channel.getStatus() != ChannelStatus.ACTIVE) {
            throw new InvalidOperationException("Kênh kết nối hiện không hoạt động");
        }

        Visitor visitor = visitorRepository.findByIdAndCompanyId(request.getVisitorId(), targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", request.getVisitorId()));

        ConversationResolutionResult result = getOrCreateActiveConversation(channel, visitor);
        return ConversationResponse.fromEntity(result.getConversation());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(String currentUserEmail, Long companyId, Long conversationId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Conversation conversation = conversationRepository.findByIdAndChannelCompanyId(conversationId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        return ConversationResponse.fromEntity(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        return conversationRepository.findByChannelCompanyId(targetCompanyId)
                .stream()
                .map(ConversationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getConversationsPaged(String currentUserEmail, Long companyId,
                                                                    ConversationStatus status, Pageable pageable) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        int validPage = Math.max(pageable.getPageNumber(), 0);
        int validSize = pageable.getPageSize() <= 0 ? 20 : Math.min(pageable.getPageSize(), 100);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable safePageable = PageRequest.of(validPage, validSize, sort);

        Page<Conversation> page = status != null
                ? conversationRepository.findByChannelCompanyIdAndStatus(targetCompanyId, status, safePageable)
                : conversationRepository.findByChannelCompanyId(targetCompanyId, safePageable);

        List<ConversationResponse> items = page.getContent()
                .stream()
                .map(ConversationResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private void validateChannelAndVisitor(Channel channel, Visitor visitor) {
        if (channel == null || channel.getId() == null) {
            throw new InvalidOperationException("Channel không hợp lệ khi tạo cuộc hội thoại");
        }
        if (visitor == null || visitor.getId() == null) {
            throw new InvalidOperationException("Visitor không hợp lệ khi tạo cuộc hội thoại");
        }
        if (channel.getCompany() != null && visitor.getCompany() != null
                && !channel.getCompany().getId().equals(visitor.getCompany().getId())) {
            throw new InvalidOperationException("Channel và Visitor không thuộc cùng một công ty");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Long resolveTargetCompanyId(Long companyId) {
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }
        return targetCompanyId;
    }

    private void validateActiveCompanyMember(Long userId, Long companyId) {
        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(userId, companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));

        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }
    }
}
