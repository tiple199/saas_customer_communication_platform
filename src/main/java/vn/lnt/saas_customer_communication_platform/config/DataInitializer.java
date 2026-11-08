package vn.lnt.saas_customer_communication_platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:superadmin@saas.com}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:Admin@123}")
    private String superAdminPassword;

    @Value("${app.super-admin.full-name:System Super Admin}")
    private String superAdminFullName;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        var existingOpt = userRepository.findByEmail(superAdminEmail);
        if (existingOpt.isEmpty()) {
            User superAdmin = new User(
                    superAdminEmail,
                    passwordEncoder.encode(superAdminPassword),
                    superAdminFullName,
                    "ROLE_SUPER_ADMIN"
            );
            superAdmin.setStatus("ACTIVE");
            userRepository.save(superAdmin);
            log.info("[DataInitializer] Đã khởi tạo tài khoản Super Admin mặc định: {}", superAdminEmail);
        } else {
            User existing = existingOpt.get();
            if (!"ROLE_SUPER_ADMIN".equals(existing.getRole())) {
                existing.setRole("ROLE_SUPER_ADMIN");
                userRepository.save(existing);
                log.info("[DataInitializer] Đã cập nhật quyền ROLE_SUPER_ADMIN cho tài khoản: {}", superAdminEmail);
            }
        }
    }
}

