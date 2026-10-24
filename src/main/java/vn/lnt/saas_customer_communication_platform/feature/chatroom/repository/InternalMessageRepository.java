package vn.lnt.saas_customer_communication_platform.feature.chatroom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.InternalMessage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InternalMessageRepository extends JpaRepository<InternalMessage, Long> {

    Page<InternalMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);

    List<InternalMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    Optional<InternalMessage> findTopByChatRoomIdOrderByIdDesc(Long chatRoomId);

    @Query("""
        SELECT m.chatRoom.id, COUNT(m)
        FROM InternalMessage m, ChatRoomMember crm
        WHERE m.chatRoom.id = crm.chatRoom.id
          AND crm.userId = :userId
          AND m.chatRoom.id IN :roomIds
          AND (crm.lastReadMessageId IS NULL OR m.id > crm.lastReadMessageId)
        GROUP BY m.chatRoom.id
    """)
    List<Object[]> countUnreadPerRoom(@Param("roomIds") Collection<Long> roomIds, @Param("userId") Long userId);
}
