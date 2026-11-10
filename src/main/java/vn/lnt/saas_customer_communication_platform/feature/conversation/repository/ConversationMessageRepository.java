package vn.lnt.saas_customer_communication_platform.feature.conversation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Page<ConversationMessage> findByConversationId(Long conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.company.id = :companyId AND m.senderType = 'AI' AND m.createdAt >= :startDate")
    long countAiMessagesSince(@Param("companyId") Long companyId, @Param("startDate") LocalDateTime startDate);
}

