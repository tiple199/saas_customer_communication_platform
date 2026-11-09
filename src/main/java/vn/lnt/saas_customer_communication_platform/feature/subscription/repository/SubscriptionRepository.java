package vn.lnt.saas_customer_communication_platform.feature.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByPlanId(Long planId);

    boolean existsByPendingPlanId(Long planId);

    Optional<Subscription> findFirstByCompanyIdAndStatusOrderByEndAtDesc(Long companyId, String status);

    Optional<Subscription> findFirstByCompanyIdOrderByCreatedAtDesc(Long companyId);
}

