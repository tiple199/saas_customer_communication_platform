package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.KnowledgeBase;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Optional<KnowledgeBase> findByIdAndCompanyId(Long id, Long companyId);

    List<KnowledgeBase> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Page<KnowledgeBase> findByCompanyId(Long companyId, Pageable pageable);

    Page<KnowledgeBase> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name, Pageable pageable);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long id);

    long countByCompanyId(Long companyId);
}

