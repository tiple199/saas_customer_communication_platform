package vn.lnt.saas_customer_communication_platform.feature.conversation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Page<ConversationMessage> findByConversationId(Long conversationId, Pageable pageable);
}

