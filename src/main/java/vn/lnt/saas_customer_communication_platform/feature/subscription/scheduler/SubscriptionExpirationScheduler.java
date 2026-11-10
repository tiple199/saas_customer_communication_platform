package vn.lnt.saas_customer_communication_platform.feature.subscription.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanService;

@Component
public class SubscriptionExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpirationScheduler.class);

    private final PlanService planService;

    public SubscriptionExpirationScheduler(PlanService planService) {
        this.planService = planService;
    }

    /**
     * Tự động quét và xử lý các subscription hết hạn mỗi đêm lúc 00:05.
     * Cron expression: "0 5 0 * * ?" (Giây=0, Phút=5, Giờ=0, Mỗi ngày, Mỗi tháng, Bất kỳ ngày trong tuần).
     */
    @Scheduled(cron = "${app.subscription.expiration-cron:0 5 0 * * ?}")
    public void runExpirationJob() {
        log.info("=== [BẮT ĐẦU CRON JOB] QUÉT VÀ XỬ LÝ HẾT HẠN GÓI CƯỚC SUBSCRIPTION ===");
        try {
            int count = planService.processExpiredSubscriptions();
            log.info("=== [KẾT THÚC CRON JOB] Đã xử lý xong {} gói cước hết hạn ===", count);
        } catch (Exception e) {
            log.error("Lỗi ngoại lệ trong quá trình chạy Expiration Scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Quét và ghi log cảnh báo các gói cước sắp hết hạn trong 3 ngày tới mỗi ngày lúc 09:00 sáng.
     */
    @Scheduled(cron = "${app.subscription.warning-cron:0 0 9 * * ?}")
    public void runExpiringSoonWarningJob() {
        log.info("=== [CRON JOB] QUÉT CẢNH BÁO CÁC GÓI CƯỚC SẮP HẾT HẠN TRONG 3 NGÀY TỚI ===");
        try {
            int count = planService.checkExpiringSubscriptions();
            log.info("Tổng số gói cước sắp hết hạn cần lưu ý: {}", count);
        } catch (Exception e) {
            log.error("Lỗi ngoại lệ trong quá trình quét cảnh báo sắp hết hạn: {}", e.getMessage(), e);
        }
    }
}

