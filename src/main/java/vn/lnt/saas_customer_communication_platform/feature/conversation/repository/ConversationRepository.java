package vn.lnt.saas_customer_communication_platform.feature.conversation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.visitor v LEFT JOIN FETCH v.company LEFT JOIN FETCH c.channel ch LEFT JOIN FETCH ch.company WHERE c.id = :id")
    Optional<Conversation> findByIdWithDetails(@Param("id") Long id);

    Optional<Conversation> findFirstByVisitorIdAndChannelIdAndStatusOrderByIdDesc(
            Long visitorId,
            Long channelId,
            ConversationStatus status
    );

    Optional<Conversation> findFirstByVisitorIdAndChannelIdOrderByIdDesc(
            Long visitorId,
            Long channelId
    );

    Optional<Conversation> findByIdAndChannelCompanyId(Long id, Long companyId);

    List<Conversation> findByChannelId(Long channelId);

    List<Conversation> findByChannelCompanyId(Long companyId);

    Page<Conversation> findByChannelCompanyId(Long companyId, Pageable pageable);

    Page<Conversation> findByChannelCompanyIdAndStatus(Long companyId, ConversationStatus status, Pageable pageable);
}
