package vn.lnt.saas_customer_communication_platform.feature.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberId;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, CompanyMemberId> {

    List<CompanyMember> findByIdUserId(Long userId);

    Optional<CompanyMember> findByIdUserIdAndIdCompanyId(Long userId, Long companyId);

    boolean existsByIdUserIdAndIdCompanyId(Long userId, Long companyId);

    boolean existsByIdUserIdAndIdCompanyIdAndStatus(Long userId, Long companyId, CompanyMemberStatus status);
}
