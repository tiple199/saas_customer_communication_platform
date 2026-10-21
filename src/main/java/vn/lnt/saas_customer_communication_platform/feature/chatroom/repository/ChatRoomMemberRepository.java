package vn.lnt.saas_customer_communication_platform.feature.chatroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.lnt.saas_customer_communication_platform.feature.chatroom.entity.ChatRoomMember;

import java.util.Collection;
import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    /**
     * Lấy thành viên còn lại (không phải {@code userId}) của các phòng được chỉ định.
     * Chỉ dùng cho phòng DIRECT nên mỗi phòng trả về đúng 1 dòng.
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.chatRoom.id IN :roomIds AND m.userId <> :userId")
    List<ChatRoomMember> findOtherMembers(@Param("roomIds") Collection<Long> roomIds, @Param("userId") Long userId);

    List<ChatRoomMember> findByUserId(Long userId);

    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
