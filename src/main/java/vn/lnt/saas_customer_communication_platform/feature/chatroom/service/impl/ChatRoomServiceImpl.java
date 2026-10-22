package vn.lnt.saas_customer_communication_platform.feature.chatroom.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.AddChatRoomMemberRequest;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.ChatRoomMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.ChatRoomResponse;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.dto.CreateChatRoomRequest;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoom;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoomMember;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoomType;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.repository.ChatRoomMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.repository.ChatRoomRepository;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.service.ChatRoomService;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;

    public ChatRoomServiceImpl(ChatRoomRepository chatRoomRepository,
                               ChatRoomMemberRepository chatRoomMemberRepository,
                               CompanyRepository companyRepository,
                               CompanyMemberRepository companyMemberRepository,
                               UserRepository userRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.userRepository = userRepository;
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
                .orElseThrow(() -> new InvalidOperationException("Bạn không có quyền truy cập công ty này"));
        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));
    }

    /**
     * Lấy phòng chat và kiểm tra phòng thuộc công ty + người dùng là thành viên của phòng.
     */
    private ChatRoom getAccessibleChatRoom(Long userId, Long companyId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom", "id", chatRoomId));

        if (chatRoom.getCompany() == null || !companyId.equals(chatRoom.getCompany().getId())) {
            throw new InvalidOperationException("Phòng chat không thuộc công ty này");
        }

        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new InvalidOperationException("Bạn không phải là thành viên của phòng chat này");
        }
        return chatRoom;
    }

    /**
     * Khóa duy nhất cho phòng DIRECT, không phụ thuộc thứ tự 2 user.
     */
    private static String buildDirectKey(Long companyId, Long userA, Long userB) {
        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);
        return companyId + ":" + min + ":" + max;
    }

    /**
     * Chuyển danh sách phòng sang response. Chỉ nạp thông tin người còn lại của các phòng DIRECT
     * (theo lô, tránh N+1); không tải danh sách thành viên của phòng.
     */
    private List<ChatRoomResponse> toResponses(List<ChatRoom> rooms, Long currentUserId) {
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> directRoomIds = rooms.stream()
                .filter(room -> room.getType() == ChatRoomType.DIRECT)
                .map(ChatRoom::getId)
                .collect(Collectors.toList());

        Map<Long, Long> partnerIdByRoom = new HashMap<>();
        Map<Long, User> partnerMap = Map.of();
        if (!directRoomIds.isEmpty()) {
            for (ChatRoomMember m : chatRoomMemberRepository.findOtherMembers(directRoomIds, currentUserId)) {
                partnerIdByRoom.put(m.getChatRoom().getId(), m.getUserId());
            }
            partnerMap = userRepository.findAllById(new HashSet<>(partnerIdByRoom.values())).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }

        final Map<Long, User> partners = partnerMap;
        return rooms.stream()
                .map(room -> room.getType() == ChatRoomType.DIRECT
                        ? ChatRoomResponse.fromEntity(room, partners.get(partnerIdByRoom.get(room.getId())))
                        : ChatRoomResponse.fromEntity(room))
                .collect(Collectors.toList());
    }

    private void addMember(ChatRoom chatRoom, Long userId) {
        ChatRoomMember member = new ChatRoomMember();
        member.setUserId(userId);
        member.setChatRoom(chatRoom);
        chatRoomMemberRepository.save(member);
    }

    /**
     * Lấy hoặc tạo phòng DIRECT giữa {@code currentUser} và {@code targetUserId} trong {@code company}.
     * Người gọi phải đảm bảo {@code currentUser} là thành viên ACTIVE của công ty.
     */
    private ChatRoomResponse getOrCreateDirectRoomInternal(User currentUser, Company company, Long targetUserId) {
        if (targetUserId == null) {
            throw new InvalidOperationException("Vui lòng chọn người dùng để bắt đầu trò chuyện");
        }
        if (targetUserId.equals(currentUser.getId())) {
            throw new InvalidOperationException("Không thể tạo phòng chat 1-1 với chính mình");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (!companyMemberRepository.existsByIdUserIdAndIdCompanyIdAndStatus(
                targetUserId, company.getId(), CompanyMemberStatus.ACTIVE)) {
            throw new InvalidOperationException("Người dùng này không phải thành viên đang hoạt động của công ty");
        }

        String directKey = buildDirectKey(company.getId(), currentUser.getId(), targetUserId);

        Optional<ChatRoom> existing = chatRoomRepository.findByDirectKey(directKey);
        if (existing.isPresent()) {
            return ChatRoomResponse.fromEntity(existing.get(), targetUser);
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(null); // Tên hiển thị được tính động theo người còn lại
        chatRoom.setType(ChatRoomType.DIRECT);
        chatRoom.setDirectKey(directKey);
        chatRoom.setCreateBy(currentUser.getId());
        chatRoom.setCompany(company);

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        addMember(savedChatRoom, currentUser.getId());
        addMember(savedChatRoom, targetUserId);

        return ChatRoomResponse.fromEntity(savedChatRoom, targetUser);
    }

    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(String currentUserEmail, Long companyId, CreateChatRoomRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        Company company = getCompany(targetCompanyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        if (request.getType() == ChatRoomType.DIRECT) {
            Set<Long> otherUserIds = request.getMemberUserIds() == null ? Set.of() :
                    request.getMemberUserIds().stream()
                            .filter(Objects::nonNull)
                            .filter(id -> !id.equals(currentUser.getId()))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            if (otherUserIds.size() != 1) {
                throw new InvalidOperationException("Phòng chat DIRECT phải có đúng 1 thành viên khác ngoài bạn");
            }
            return getOrCreateDirectRoomInternal(currentUser, company, otherUserIds.iterator().next());
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(request.getName() != null && !request.getName().trim().isEmpty() ? request.getName().trim() : "Phòng chat mới");
        chatRoom.setType(ChatRoomType.GROUP);
        chatRoom.setCreateBy(currentUser.getId());
        chatRoom.setCompany(company);

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        Set<Long> memberUserIds = new LinkedHashSet<>();
        memberUserIds.add(currentUser.getId());

        if (request.getMemberUserIds() != null) {
            for (Long userId : request.getMemberUserIds()) {
                if (userId != null && companyMemberRepository.existsByIdUserIdAndIdCompanyIdAndStatus(userId, targetCompanyId, CompanyMemberStatus.ACTIVE)) {
                    memberUserIds.add(userId);
                }
            }
        }

        for (Long userId : memberUserIds) {
            addMember(savedChatRoom, userId);
        }

        return ChatRoomResponse.fromEntity(savedChatRoom);
    }

    @Override
    @Transactional
    public ChatRoomResponse getOrCreateDirectRoom(String currentUserEmail, Long companyId, Long targetUserId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        Company company = getCompany(targetCompanyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        return getOrCreateDirectRoomInternal(currentUser, company, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        List<ChatRoomMember> myMemberships = chatRoomMemberRepository.findByUserId(currentUser.getId());
        if (myMemberships.isEmpty()) {
            return List.of();
        }

        List<ChatRoom> companyRooms = myMemberships.stream()
                .map(ChatRoomMember::getChatRoom)
                .filter(room -> room != null && room.getCompany() != null && targetCompanyId.equals(room.getCompany().getId()))
                .collect(Collectors.toList());

        return toResponses(companyRooms, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomById(String currentUserEmail, Long companyId, Long chatRoomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        ChatRoom chatRoom = getAccessibleChatRoom(currentUser.getId(), targetCompanyId, chatRoomId);
        return toResponses(List.of(chatRoom), currentUser.getId()).get(0);
    }

    private List<ChatRoomMemberResponse> getChatRoomMembersInternal(Long chatRoomId) {
        List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        List<Long> userIds = roomMembers.stream().map(ChatRoomMember::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return roomMembers.stream()
                .map(m -> ChatRoomMemberResponse.fromEntity(m, userMap.get(m.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomMemberResponse> getChatRoomMembers(String currentUserEmail, Long companyId, Long chatRoomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        getAccessibleChatRoom(currentUser.getId(), targetCompanyId, chatRoomId);

        return getChatRoomMembersInternal(chatRoomId);
    }

    @Override
    @Transactional
    public void addMembers(String currentUserEmail, Long companyId, Long chatRoomId, AddChatRoomMemberRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        ChatRoom chatRoom = getAccessibleChatRoom(currentUser.getId(), targetCompanyId, chatRoomId);

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new InvalidOperationException("Chỉ có thể thêm thành viên vào phòng chat nhóm");
        }

        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new InvalidOperationException("Danh sách thành viên cần thêm không được để trống");
        }

        Set<Long> uniqueUserIds = request.getUserIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueUserIds.isEmpty()) {
            throw new InvalidOperationException("Danh sách thành viên cần thêm không được để trống");
        }

        List<Long> toAdd = new ArrayList<>();
        for (Long userId : uniqueUserIds) {
            if (!companyMemberRepository.existsByIdUserIdAndIdCompanyIdAndStatus(
                    userId, targetCompanyId, CompanyMemberStatus.ACTIVE)) {
                throw new InvalidOperationException("Người dùng với ID " + userId + " không phải thành viên đang hoạt động của công ty");
            }
            if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
                toAdd.add(userId);
            }
        }

        if (toAdd.isEmpty()) {
            throw new InvalidOperationException("Tất cả người dùng được chọn đã là thành viên của phòng chat");
        }

        for (Long userId : toAdd) {
            addMember(chatRoom, userId);
        }
    }

    @Override
    @Transactional
    public void removeMember(String currentUserEmail, Long companyId, Long chatRoomId, Long targetUserId) {
        if (targetUserId == null) {
            throw new InvalidOperationException("Vui lòng chọn thành viên cần xóa");
        }

        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        ChatRoom chatRoom = getAccessibleChatRoom(currentUser.getId(), targetCompanyId, chatRoomId);

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new InvalidOperationException("Chỉ có thể xóa thành viên khỏi phòng chat nhóm");
        }

        ChatRoomMember memberToRemove = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .orElseThrow(() -> new InvalidOperationException("Người dùng này không phải là thành viên của phòng chat"));

        boolean isSelf = currentUser.getId().equals(targetUserId);
        boolean isCreator = chatRoom.getCreateBy() != null && chatRoom.getCreateBy().equals(currentUser.getId());
        boolean targetIsCreator = chatRoom.getCreateBy() != null && chatRoom.getCreateBy().equals(targetUserId);

        if (isSelf) {
            if (targetIsCreator) {
                throw new InvalidOperationException("Người tạo nhóm không thể tự rời nhóm");
            }
        } else {
            if (!isCreator) {
                throw new InvalidOperationException("Chỉ người tạo nhóm mới có quyền xóa thành viên khác khỏi nhóm");
            }
            if (targetIsCreator) {
                throw new InvalidOperationException("Không thể xóa người tạo nhóm khỏi phòng chat");
            }
        }

        chatRoomMemberRepository.delete(memberToRemove);
    }
}
