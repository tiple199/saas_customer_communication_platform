package vn.lnt.saas_customer_communication_platform.feature.conversation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;

import java.util.Optional;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    Optional<Visitor> findByVisitorToken(String visitorToken);

    Optional<Visitor> findByVisitorTokenAndCompanyId(String visitorToken, Long companyId);
}

