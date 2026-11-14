package vn.lnt.saas_customer_communication_platform.feature.company.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;

import java.time.LocalDateTime;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT COUNT(c) FROM Company c WHERE c.createdAt >= :since")
    long countNewCompaniesSince(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Company c WHERE c.createdAt >= :since ORDER BY c.createdAt ASC")
    List<Company> findCompaniesCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Company c ORDER BY c.createdAt DESC")
    List<Company> findTopRecentCompanies(Pageable pageable);

    @Query("SELECT c FROM Company c WHERE " +
           "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<Company> searchCompanies(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
