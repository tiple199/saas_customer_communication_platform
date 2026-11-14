package vn.lnt.saas_customer_communication_platform.feature.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByPlanId(Long planId);

    boolean existsByPendingPlanId(Long planId);

    Optional<Subscription> findFirstByCompanyIdAndStatusOrderByEndAtDesc(Long companyId, String status);

    Optional<Subscription> findFirstByCompanyIdOrderByCreatedAtDesc(Long companyId);

    long countByStatus(String status);

    @Query("SELECT s.plan.id, s.plan.name, s.plan.price, COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' GROUP BY s.plan.id, s.plan.name, s.plan.price")
    List<Object[]> getActivePlanDistribution();

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endAt IS NOT NULL AND s.endAt < :now AND s.plan.price > 0")
    List<Subscription> findExpiredPaidSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endAt IS NOT NULL AND s.endAt >= :start AND s.endAt <= :end AND s.plan.price > 0")
    List<Subscription> findExpiringPaidSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Subscription s WHERE " +
           "(:keyword IS NULL OR LOWER(s.company.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.plan.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:planId IS NULL OR s.plan.id = :planId) AND " +
           "(:isPaid IS NULL OR (:isPaid = true AND s.plan.price > 0) OR (:isPaid = false AND s.plan.price = 0)) AND " +
           "(:isExpiringSoon IS NULL OR (:isExpiringSoon = true AND s.endAt IS NOT NULL AND s.endAt >= :now AND s.endAt <= :expiringThreshold))")
    Page<Subscription> searchSubscriptions(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("planId") Long planId,
            @Param("isPaid") Boolean isPaid,
            @Param("isExpiringSoon") Boolean isExpiringSoon,
            @Param("now") LocalDateTime now,
            @Param("expiringThreshold") LocalDateTime expiringThreshold,
            Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.plan.price > 0")
    long countActivePaidSubscriptions();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.plan.price = 0")
    long countActiveFreeSubscriptions();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endAt IS NOT NULL AND s.endAt >= :start AND s.endAt <= :end AND s.plan.price > 0")
    long countExpiringSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.plan.price), 0) FROM Subscription s WHERE s.status = 'ACTIVE'")
    BigDecimal sumActiveMonthlyRevenue();
}
