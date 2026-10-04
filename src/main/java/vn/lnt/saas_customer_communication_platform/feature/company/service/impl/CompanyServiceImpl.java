package vn.lnt.saas_customer_communication_platform.feature.company.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateCompanyRequest;
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

    public CompanyServiceImpl(CompanyRepository companyRepository,
                               CompanyMemberRepository companyMemberRepository,
                               UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
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

        if (currentUser.getCurrentCompanyId() == null) {
            currentUser.setCurrentCompanyId(savedCompany.getId());
            userRepository.save(currentUser);
        }

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
                .collect(Collectors.toMap(c -> c.getId(), Function.identity()));

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

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không có quyền truy cập công ty này"));

        return CompanyResponse.fromEntity(company, member.getRole(), member.getJoinedAt());
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(String currentUserEmail, Long companyId, UpdateCompanyRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

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

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        CompanyMember member = companyMemberRepository.findByIdUserIdAndIdCompanyId(currentUser.getId(), companyId)
                .orElseThrow(() -> new InvalidOperationException("Bạn không thuộc công ty này"));

        currentUser.setCurrentCompanyId(companyId);
        userRepository.save(currentUser);

        return CompanyResponse.fromEntity(company, member.getRole(), member.getJoinedAt());
    }
}
