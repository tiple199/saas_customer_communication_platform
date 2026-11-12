package vn.lnt.saas_customer_communication_platform.feature.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    Optional<Payment> findByTransactionRef(String transactionRef);

    Optional<Payment> findFirstByTransactionRefContaining(String keyword);

    List<Payment> findBySubscriptionCompanyIdOrderByCreatedAtDesc(Long companyId);

    Page<Payment> findBySubscriptionCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    boolean existsByTransactionRef(String transactionRef);
}

