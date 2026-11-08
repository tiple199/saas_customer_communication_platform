package vn.lnt.saas_customer_communication_platform.feature.subscription.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Plan> findByStatusOrderByPriceAsc(PlanStatus status);

    List<Plan> findAllByOrderByPriceAsc();

    Page<Plan> findByStatus(PlanStatus status, Pageable pageable);
}

