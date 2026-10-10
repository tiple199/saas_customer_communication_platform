package vn.lnt.saas_customer_communication_platform.feature.company.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.exception.DuplicateResourceException;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.AddMemberRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateMemberAccountRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateMemberStatusRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.service.CompanyService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                               CompanyMemberRepository companyMemberRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    private CompanyMember getActiveAdminOrOwnerMember(User currentUser, Long companyId, String actionErrorMessage) {
        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));

        if (member.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }

        if (member.getRole() != CompanyMemberRole.OWNER && member.getRole() != CompanyMemberRole.ADMIN) {
            throw new InvalidOperationException(actionErrorMessage);
        }

        return member;
    }

    private void validateHigherPriority(CompanyMemberRole inviterRole, CompanyMemberRole targetRole, String errorMessage) {
        if (inviterRole.getPriority() <= targetRole.getPriority()) {
            throw new InvalidOperationException(errorMessage);
        }
    }

    private CompanyMember saveOrReactivateMember(User targetUser, Long companyId, CompanyMemberRole role) {
        CompanyMember memberToSave;
        var existingMemberOpt = companyMemberRepository.findByIdUserIdAndIdCompanyId(targetUser.getId(), companyId);
        if (existingMemberOpt.isPresent()) {
            CompanyMember existingMember = existingMemberOpt.get();
            if (existingMember.getStatus() == CompanyMemberStatus.ACTIVE) {
                throw new DuplicateResourceException("CompanyMember", "email", targetUser.getEmail());
            }
            existingMember.setStatus(CompanyMemberStatus.ACTIVE);
            existingMember.setRole(role);
            memberToSave = existingMember;
        } else {
            memberToSave = new CompanyMember(
                    targetUser.getId(),
                    companyId,
                    role,
                    CompanyMemberStatus.ACTIVE
            );
        }
        return companyMemberRepository.save(memberToSave);
    }

    private void updateTargetUserCompanyIfNull(User targetUser, Long companyId) {
        if (targetUser.getCurrentCompanyId() == null) {
            targetUser.setCurrentCompanyId(companyId);
            userRepository.save(targetUser);
        }
    }

    @Override
    @Transactional
    public CompanyResponse createCompany(String currentUserEmail, CreateCompanyRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);

        Company company = new Company(
                request.getName(),
                null,
                "ACTIVE"
        );
        Company savedCompany = companyRepository.save(company);

        CompanyMember member = new CompanyMember(
                currentUser.getId(),
                savedCompany.getId(),
                CompanyMemberRole.OWNER,
                CompanyMemberStatus.ACTIVE
        );
        CompanyMember savedMember = companyMemberRepository.save(member);

        updateTargetUserCompanyIfNull(currentUser, savedCompany.getId());

        return CompanyResponse.fromEntity(savedCompany, savedMember.getRole(), savedMember.getJoinedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getMyCompanies(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);

        List<CompanyMember> memberships = companyMemberRepository.findByIdUserId(currentUser.getId());
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> companyIds = memberships.stream()
                .map(m -> m.getId().getCompanyId())
                .collect(Collectors.toList());

        Map<Long, Company> companyMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));

        return memberships.stream()
                .filter(m -> companyMap.containsKey(m.getId().getCompanyId()))
                .map(m -> {
                    Company company = companyMap.get(m.getId().getCompanyId());
                    return CompanyResponse.fromEntity(company, m.getRole(), m.getJoinedAt());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Company company = getCompanyByIdOrThrow(companyId);

        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không có quyền truy cập công ty này"));

        return CompanyResponse.fromEntity(company, member.getRole(), member.getJoinedAt());
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(String currentUserEmail, Long companyId, UpdateCompanyRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Company company = getCompanyByIdOrThrow(companyId);

        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không phải là thành viên của công ty này"));

        if (member.getRole() != CompanyMemberRole.OWNER && member.getRole() != CompanyMemberRole.ADMIN) {
            throw new InvalidOperationException("Chỉ người sở hữu hoặc quản trị viên mới có quyền cập nhật thông tin công ty");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            company.setName(request.getName().trim());
        }
        if (request.getLogoUrl() != null) {
            company.setLogoUrl(request.getLogoUrl());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            company.setStatus(request.getStatus().trim());
        }

        Company updatedCompany = companyRepository.save(company);
        return CompanyResponse.fromEntity(updatedCompany, member.getRole(), member.getJoinedAt());
    }

    @Override
    @Transactional
    public CompanyResponse switchCompany(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Company company = getCompanyByIdOrThrow(companyId);

        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không thuộc công ty này"));

        currentUser.setCurrentCompanyId(companyId);
        userRepository.save(currentUser);

        return CompanyResponse.fromEntity(company, member.getRole(), member.getJoinedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyMemberResponse> getCompanyMembers(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);

        CompanyMember currentMember = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), targetCompanyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không có quyền xem danh sách thành viên của công ty này"));

        if (currentMember.getStatus() != CompanyMemberStatus.ACTIVE) {
            throw new InvalidOperationException("Tài khoản của bạn không ở trạng thái hoạt động trong công ty này");
        }

        List<CompanyMember> members = companyMemberRepository.findByIdCompanyId(targetCompanyId);
        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = members.stream()
                .map(m -> m.getId().getUserId())
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(m -> CompanyMemberResponse.fromEntity(m, userMap.get(m.getId().getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompanyMemberResponse addMember(String currentUserEmail, Long companyId, AddMemberRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        getCompanyByIdOrThrow(targetCompanyId);

        getActiveAdminOrOwnerMember(currentUser, targetCompanyId,
                "Chỉ người sở hữu hoặc quản trị viên mới có quyền thêm thành viên vào công ty");

        User targetUser = getUserByEmail(request.getEmail());
        CompanyMember savedMember = saveOrReactivateMember(targetUser, targetCompanyId, request.getRole());
        updateTargetUserCompanyIfNull(targetUser, targetCompanyId);

        return CompanyMemberResponse.fromEntity(savedMember, targetUser);
    }

    @Override
    @Transactional
    public void removeMember(String currentUserEmail, Long companyId, Long memberUserId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        getCompanyByIdOrThrow(targetCompanyId);

        CompanyMember inviterMember = getActiveAdminOrOwnerMember(currentUser, targetCompanyId,
                "Chỉ người sở hữu hoặc quản trị viên mới có quyền xóa thành viên");

        CompanyMember targetMember = companyMemberRepository.findByIdUserIdAndIdCompanyId(memberUserId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyMember", "userId", memberUserId));

        validateHigherPriority(inviterMember.getRole(), targetMember.getRole(),
                "Bạn chỉ được phép xóa thành viên có cấp bậc thấp hơn mình");

        companyMemberRepository.delete(targetMember);

        User targetUser = userRepository.findById(memberUserId).orElse(null);
        if (targetUser != null && targetCompanyId.equals(targetUser.getCurrentCompanyId())) {
            targetUser.setCurrentCompanyId(null);
            userRepository.save(targetUser);
        }
    }

    @Override
    @Transactional
    public CompanyMemberResponse updateMemberStatus(String currentUserEmail, Long companyId, Long memberUserId, UpdateMemberStatusRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        getCompanyByIdOrThrow(targetCompanyId);

        CompanyMember inviterMember = getActiveAdminOrOwnerMember(currentUser, targetCompanyId,
                "Chỉ người sở hữu hoặc quản trị viên mới có quyền cập nhật trạng thái thành viên");

        CompanyMember targetMember = companyMemberRepository.findByIdUserIdAndIdCompanyId(memberUserId, targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyMember", "userId", memberUserId));

        validateHigherPriority(inviterMember.getRole(), targetMember.getRole(),
                "Bạn chỉ được phép cập nhật trạng thái thành viên có cấp bậc thấp hơn mình");

        targetMember.setStatus(request.getStatus());
        CompanyMember updatedMember = companyMemberRepository.save(targetMember);

        User targetUser = userRepository.findById(memberUserId).orElse(null);
        if (targetUser != null) {
            if (request.getStatus() == CompanyMemberStatus.INACTIVE && targetCompanyId.equals(targetUser.getCurrentCompanyId())) {
                targetUser.setCurrentCompanyId(null);
                userRepository.save(targetUser);
            } else if (request.getStatus() == CompanyMemberStatus.ACTIVE && targetUser.getCurrentCompanyId() == null) {
                targetUser.setCurrentCompanyId(targetCompanyId);
                userRepository.save(targetUser);
            }
        }

        return CompanyMemberResponse.fromEntity(updatedMember, targetUser);
    }

    @Override
    @Transactional
    public CompanyMemberResponse createMemberAccount(String currentUserEmail, Long companyId, CreateMemberAccountRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = resolveTargetCompanyId(companyId);
        getCompanyByIdOrThrow(targetCompanyId);

        CompanyMember inviterMember = getActiveAdminOrOwnerMember(currentUser, targetCompanyId,
                "Chỉ người sở hữu hoặc quản trị viên mới có quyền tạo tài khoản thành viên");

        validateHigherPriority(inviterMember.getRole(), request.getRole(),
                "Bạn chỉ được phép cấp tài khoản với vai trò thấp hơn mình");

        User targetUser;
        var existingUserOpt = userRepository.findByEmail(request.getEmail());
        if (existingUserOpt.isPresent()) {
            targetUser = existingUserOpt.get();
        } else {
            targetUser = new User(
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword()),
                    request.getFullName(),
                    "ROLE_USER"
            );
            targetUser.setCurrentCompanyId(targetCompanyId);
            targetUser = userRepository.save(targetUser);
        }

        CompanyMember savedMember = saveOrReactivateMember(targetUser, targetCompanyId, request.getRole());
        updateTargetUserCompanyIfNull(targetUser, targetCompanyId);

        return CompanyMemberResponse.fromEntity(savedMember, targetUser);
    }
}
