package vn.lnt.saas_customer_communication_platform.feature.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
