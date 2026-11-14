package vn.lnt.saas_customer_communication_platform.feature.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt >= :since")
    BigDecimal calculateRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt >= :since ORDER BY p.paidAt ASC")
    List<Payment> findSuccessfulPaymentsSince(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Payment p ORDER BY p.createdAt DESC")
    List<Payment> findTopRecentPayments(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.subscription.company.id = :companyId AND p.status = 'SUCCESS'")
    BigDecimal sumPaidAmountByCompanyId(@Param("companyId") Long companyId);
}
