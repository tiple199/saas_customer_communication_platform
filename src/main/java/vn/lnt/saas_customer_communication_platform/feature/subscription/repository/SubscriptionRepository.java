package vn.lnt.saas_customer_communication_platform.feature.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByPlanId(Long planId);

    boolean existsByPendingPlanId(Long planId);

    Optional<Subscription> findFirstByCompanyIdAndStatusOrderByEndAtDesc(Long companyId, String status);

    Optional<Subscription> findFirstByCompanyIdOrderByCreatedAtDesc(Long companyId);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endAt IS NOT NULL AND s.endAt < :now AND s.plan.price > 0")
    List<Subscription> findExpiredPaidSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endAt IS NOT NULL AND s.endAt >= :start AND s.endAt <= :end AND s.plan.price > 0")
    List<Subscription> findExpiringPaidSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

