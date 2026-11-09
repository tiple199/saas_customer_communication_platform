package vn.lnt.saas_customer_communication_platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:superadmin@saas.com}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:Admin@123}")
    private String superAdminPassword;

    @Value("${app.super-admin.full-name:System Super Admin}")
    private String superAdminFullName;

    public DataInitializer(UserRepository userRepository,
                           PlanRepository planRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
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

        seedSamplePlansIfEmpty();
    }

    private void seedSamplePlansIfEmpty() {
        if (planRepository.count() == 0) {
            Plan freePlan = new Plan();
            freePlan.setName("Starter (Miễn phí)");
            freePlan.setPrice(new java.math.BigDecimal("0"));
            freePlan.setMaxUser(3);
            freePlan.setMaxChannel(1);
            freePlan.setAiEnabled(false);
            freePlan.setAiQuota(0);
            freePlan.setStorageLimit(100L);
            freePlan.setStatus(vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus.ACTIVE);
            planRepository.save(freePlan);

            Plan proPlan = new Plan();
            proPlan.setName("Professional (Pro)");
            proPlan.setPrice(new java.math.BigDecimal("499000"));
            proPlan.setMaxUser(15);
            proPlan.setMaxChannel(5);
            proPlan.setAiEnabled(true);
            proPlan.setAiQuota(5000);
            proPlan.setStorageLimit(2048L);
            proPlan.setStatus(vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus.ACTIVE);
            planRepository.save(proPlan);

            Plan enterprisePlan = new Plan();
            enterprisePlan.setName("Enterprise (Doanh nghiệp)");
            enterprisePlan.setPrice(new java.math.BigDecimal("1499000"));
            enterprisePlan.setMaxUser(100);
            enterprisePlan.setMaxChannel(20);
            enterprisePlan.setAiEnabled(true);
            enterprisePlan.setAiQuota(50000);
            enterprisePlan.setStorageLimit(10240L);
            enterprisePlan.setStatus(vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus.ACTIVE);
            planRepository.save(enterprisePlan);

            log.info("[DataInitializer] Đã tự động khởi tạo 3 gói dịch vụ mẫu (Starter, Pro, Enterprise)");
        }
    }
}

