package vn.lnt.saas_customer_communication_platform.feature.chatroom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.InternalMessage;

import java.util.List;

public interface InternalMessageRepository extends JpaRepository<InternalMessage, Long> {

    Page<InternalMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);

    List<InternalMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
}
