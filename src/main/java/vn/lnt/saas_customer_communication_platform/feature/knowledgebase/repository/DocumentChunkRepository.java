package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.DocumentChunk;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    void deleteByDocumentIdIn(List<Long> documentIds);

    @Query("SELECT c FROM DocumentChunk c JOIN FETCH c.document d WHERE c.company.id = :companyId AND d.knowledgeBase.id = :knowledgeBaseId AND d.status = vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.DocumentStatus.READY")
    List<DocumentChunk> findByCompanyIdAndKnowledgeBaseId(@Param("companyId") Long companyId, @Param("knowledgeBaseId") Long knowledgeBaseId);

    @Query("SELECT c FROM DocumentChunk c JOIN FETCH c.document d WHERE c.company.id = :companyId AND d.status = vn.lnt.saas_customer_communication_platform.feature.knowledgebase.entity.DocumentStatus.READY")
    List<DocumentChunk> findReadyChunksByCompanyId(@Param("companyId") Long companyId);
}

