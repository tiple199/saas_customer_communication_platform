package vn.lnt.saas_customer_communication_platform.feature.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;


public interface CompanyRepository extends JpaRepository<Company, Long> {
}
