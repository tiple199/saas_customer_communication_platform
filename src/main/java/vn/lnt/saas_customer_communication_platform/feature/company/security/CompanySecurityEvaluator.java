package vn.lnt.saas_customer_communication_platform.feature.company.security;

import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;

@Component("companySecurity")
public class CompanySecurityEvaluator {

    private final CompanyMemberRepository companyMemberRepository;

    public CompanySecurityEvaluator(CompanyMemberRepository companyMemberRepository) {
        this.companyMemberRepository = companyMemberRepository;
    }

    public boolean isMember(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return false;
        }
        return companyMemberRepository.existsByIdUserIdAndIdCompanyIdAndStatus(
                userId, companyId, CompanyMemberStatus.ACTIVE);
    }

    public boolean isOwnerOrAdmin(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return false;
        }
        return companyMemberRepository.findByIdUserIdAndIdCompanyId(userId, companyId)
                .map(member -> member.getStatus() == CompanyMemberStatus.ACTIVE &&
                        (member.getRole() == CompanyMemberRole.OWNER || member.getRole() == CompanyMemberRole.ADMIN))
                .orElse(false);
    }
}
