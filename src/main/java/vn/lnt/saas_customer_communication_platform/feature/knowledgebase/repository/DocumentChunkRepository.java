package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.DocumentChunk;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    void deleteByDocumentIdIn(List<Long> documentIds);
}

