package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.Document;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndCompanyId(Long id, Long companyId);

    List<Document> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);

    Page<Document> findByKnowledgeBaseId(Long knowledgeBaseId, Pageable pageable);

    Page<Document> findByKnowledgeBaseIdAndFileNameContainingIgnoreCase(Long knowledgeBaseId, String fileName, Pageable pageable);

    Page<Document> findByCompanyId(Long companyId, Pageable pageable);

    long countByKnowledgeBaseId(Long knowledgeBaseId);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}

