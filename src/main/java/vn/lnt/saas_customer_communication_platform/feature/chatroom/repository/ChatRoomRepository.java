package vn.lnt.saas_customer_communication_platform.feature.chatroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByCompanyId(Long companyId);

    Optional<ChatRoom> findByDirectKey(String directKey);
}
