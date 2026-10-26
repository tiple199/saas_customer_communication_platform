package vn.lnt.saas_customer_communication_platform.feature.channel.service.impl;

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
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.ChannelResponse;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.service.ChannelService;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                              UserRepository userRepository,
                              CompanyRepository companyRepository,
                              CompanyMemberRepository companyMemberRepository) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
    }

    @Override
    @Transactional
    public ChannelResponse createChannel(String currentUserEmail, Long companyId, CreateChannelRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        Company company = getCompanyByIdOrThrow(targetCompanyId);

        // Chỉ OWNER hoặc ADMIN của công ty mới được phép tạo Channel
        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền tạo kênh kết nối");

        // Kiểm tra trùng tên channel trong công ty
        if (channelRepository.existsByCompanyIdAndName(targetCompanyId, request.getName().trim())) {
            throw new InvalidOperationException("Tên kênh kết nối đã tồn tại trong công ty này");
        }

        Channel channel = new Channel();
        channel.setName(request.getName().trim());
        channel.setType(request.getType().trim().toUpperCase());
        channel.setStatus("ACTIVE");
        channel.setConfig(request.getConfig());
        channel.setCompany(company);

        Channel savedChannel = channelRepository.save(channel);
        return ChannelResponse.fromEntity(savedChannel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannels(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        // Thành viên ACTIVE của công ty có quyền xem danh sách channel
        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        return channelRepository.findByCompanyId(targetCompanyId)
                .stream()
                .map(ChannelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChannelResponse> getChannelsPaged(String currentUserEmail, Long companyId, Pageable pageable) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        int validPage = Math.max(pageable.getPageNumber(), 0);
        int validSize = pageable.getPageSize() <= 0 ? 20 : Math.min(pageable.getPageSize(), 100);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(validPage, validSize, sort);

        Page<Channel> channelPage = channelRepository.findByCompanyId(targetCompanyId, safePageable);
        List<ChannelResponse> content = channelPage.getContent()
                .stream()
                .map(ChannelResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                channelPage.getNumber(),
                channelPage.getSize(),
                channelPage.getTotalElements(),
                channelPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(String currentUserEmail, Long companyId, Long channelId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveCompanyMember(currentUser.getId(), targetCompanyId);

        Channel channel = channelRepository.findByIdAndCompanyId(channelId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        return ChannelResponse.fromEntity(channel);
    }

    @Override
    @Transactional
    public ChannelResponse updateChannel(String currentUserEmail, Long companyId, Long channelId, UpdateChannelRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền cập nhật kênh kết nối");

        Channel channel = channelRepository.findByIdAndCompanyId(channelId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (channelRepository.existsByCompanyIdAndNameAndIdNot(targetCompanyId, newName, channelId)) {
                throw new InvalidOperationException("Tên kênh kết nối đã tồn tại trong công ty này");
            }
            channel.setName(newName);
        }

        if (request.getType() != null && !request.getType().isBlank()) {
            channel.setType(request.getType().trim().toUpperCase());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            channel.setStatus(request.getStatus().trim().toUpperCase());
        }

        if (request.getConfig() != null) {
            channel.setConfig(request.getConfig());
        }

        Channel updatedChannel = channelRepository.save(channel);
        return ChannelResponse.fromEntity(updatedChannel);
    }

    @Override
    @Transactional
    public void deleteChannel(String currentUserEmail, Long companyId, Long channelId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        validateActiveAdminOrOwner(currentUser.getId(), targetCompanyId, "Chỉ quản trị viên hoặc chủ công ty mới có quyền xóa kênh kết nối");

        Channel channel = channelRepository.findByIdAndCompanyId(channelId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        channelRepository.delete(channel);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Company getCompanyByIdOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));
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

    private void validateActiveAdminOrOwner(Long userId, Long companyId, String actionErrorMessage) {
        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(userId, companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));

        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }

        if (member.getRole() != CompanyMemberRole.OWNER && member.getRole() != CompanyMemberRole.ADMIN) {
            throw new InvalidOperationException(actionErrorMessage);
        }
    }
}
