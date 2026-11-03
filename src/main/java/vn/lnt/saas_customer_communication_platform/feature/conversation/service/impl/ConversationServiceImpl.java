package vn.lnt.saas_customer_communication_platform.feature.conversation.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.CreateConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.HandlingMode;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationMessageRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.VisitorRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.ConversationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ChannelRepository channelRepository;
    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   ConversationMessageRepository conversationMessageRepository,
                                   ChannelRepository channelRepository,
                                   VisitorRepository visitorRepository,
                                   UserRepository userRepository,
                                   CompanyMemberRepository companyMemberRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.channelRepository = channelRepository;
        this.visitorRepository = visitorRepository;
        this.userRepository = userRepository;
        this.companyMemberRepository = companyMemberRepository;
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

        return conversationRepository.save(conversation);
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

        String normalizedType = (messageType == null || messageType.isBlank())
                ? "TEXT"
                : messageType.trim().toUpperCase();

        ConversationMessage message = new ConversationMessage();
        message.setCompany(channel.getCompany());
        message.setConversation(conversation);
        message.setSenderType("VISITOR");
        message.setSenderId(visitor.getId());
        message.setContent(content.trim());
        message.setMessageType(normalizedType);

        ConversationMessage savedMessage = conversationMessageRepository.save(message);
        return ConversationMessageResponse.fromEntity(savedMessage);
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
