package vn.lnt.saas_customer_communication_platform.feature.channel.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByCompanyId(Long companyId);

    Page<Channel> findByCompanyId(Long companyId, Pageable pageable);

    Optional<Channel> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Channel> findByChannelKey(String channelKey);

    Optional<Channel> findByChannelKeyAndCompanyId(String channelKey, Long companyId);

    boolean existsByChannelKey(String channelKey);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long id);

    long countByCompanyId(Long companyId);
}
