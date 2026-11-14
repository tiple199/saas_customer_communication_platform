package vn.lnt.saas_customer_communication_platform.feature.company.security;

import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;

@Component("companySecurity")
public class CompanySecurityEvaluator {

    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyRepository companyRepository;

    public CompanySecurityEvaluator(CompanyMemberRepository companyMemberRepository,
                                   CompanyRepository companyRepository) {
        this.companyMemberRepository = companyMemberRepository;
        this.companyRepository = companyRepository;
    }

    public boolean isCompanyActive(Long companyId) {
        if (companyId == null) {
            return false;
        }
        return companyRepository.findById(companyId)
                .map(c -> !"SUSPENDED".equalsIgnoreCase(c.getStatus()) && !"INACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElse(false);
    }

    public boolean isMember(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return false;
        }
        if (!isCompanyActive(companyId)) {
            return false;
        }
        return companyMemberRepository.existsByIdUserIdAndIdCompanyIdAndStatus(
                userId, companyId, CompanyMemberStatus.ACTIVE);
    }

    public boolean isOwnerOrAdmin(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return false;
        }
        if (!isCompanyActive(companyId)) {
            return false;
        }
        return companyMemberRepository.findByIdUserIdAndIdCompanyId(userId, companyId)
                .map(member -> member.getStatus() == CompanyMemberStatus.ACTIVE &&
                        (member.getRole() == CompanyMemberRole.OWNER || member.getRole() == CompanyMemberRole.ADMIN))
                .orElse(false);
    }
}
