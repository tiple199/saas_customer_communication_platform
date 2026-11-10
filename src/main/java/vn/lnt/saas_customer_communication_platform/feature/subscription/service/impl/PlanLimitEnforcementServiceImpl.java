package vn.lnt.saas_customer_communication_platform.feature.subscription.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationMessageRepository;
import vn.lnt.saas_customer_communication_platform.feature.knowledgebase.repository.DocumentRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UsageStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanLimitEnforcementService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PlanLimitEnforcementServiceImpl implements PlanLimitEnforcementService {

    private static final Logger log = LoggerFactory.getLogger(PlanLimitEnforcementServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final ChannelRepository channelRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final DocumentRepository documentRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    public PlanLimitEnforcementServiceImpl(SubscriptionRepository subscriptionRepository,
                                           PlanRepository planRepository,
                                           ChannelRepository channelRepository,
                                           CompanyMemberRepository companyMemberRepository,
                                           DocumentRepository documentRepository,
                                           ConversationMessageRepository conversationMessageRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.channelRepository = channelRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.documentRepository = documentRepository;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Plan getEffectivePlan(Long companyId) {
        if (companyId == null) {
            return getDefaultStarterPlan();
        }

        Subscription sub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(companyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId))
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        // Nếu gói trả phí hiện tại đã hết hạn (endAt < now), xem pendingPlanId hoặc về Starter
        if (sub != null && sub.getPlan() != null && sub.getPlan().getPrice() != null
                && sub.getPlan().getPrice().compareTo(BigDecimal.ZERO) > 0) {
            if (sub.getEndAt() != null && sub.getEndAt().isBefore(now)) {
                if (sub.getPendingPlanId() != null) {
                    Plan pending = planRepository.findById(sub.getPendingPlanId()).orElse(null);
                    if (pending != null) {
                        return pending;
                    }
                }
                return getDefaultStarterPlan();
            }
        }

        if (sub != null && sub.getPlan() != null) {
            return sub.getPlan();
        }

        return getDefaultStarterPlan();
    }

    private Plan getDefaultStarterPlan() {
        return planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "status", PlanStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanCreateChannel(Long companyId, Boolean requestAiEnabled) {
        Plan plan = getEffectivePlan(companyId);
        if (plan.getMaxChannel() != null) {
            long activeChannels = channelRepository.countByCompanyIdAndStatus(companyId, ChannelStatus.ACTIVE);
            if (activeChannels >= plan.getMaxChannel()) {
                throw new InvalidOperationException(String.format(
                        "Công ty đã đạt giới hạn tối đa số kênh hoạt động (%d/%d kênh) của gói %s. Vui lòng nâng cấp gói dịch vụ để thêm kênh mới.",
                        activeChannels, plan.getMaxChannel(), plan.getName()
                ));
            }
        }

        if (Boolean.TRUE.equals(requestAiEnabled) && !Boolean.TRUE.equals(plan.getAiEnabled())) {
            throw new InvalidOperationException(String.format(
                    "Gói dịch vụ %s không hỗ trợ tính năng AI Auto-Reply. Vui lòng tắt tùy chọn AI hoặc nâng cấp lên gói Pro/Enterprise.",
                    plan.getName()
            ));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanActivateChannel(Long companyId) {
        Plan plan = getEffectivePlan(companyId);
        if (plan.getMaxChannel() != null) {
            long activeChannels = channelRepository.countByCompanyIdAndStatus(companyId, ChannelStatus.ACTIVE);
            if (activeChannels >= plan.getMaxChannel()) {
                throw new InvalidOperationException(String.format(
                        "Không thể kích hoạt kênh. Công ty đã đạt giới hạn tối đa số kênh hoạt động (%d/%d kênh) của gói %s. Vui lòng nâng cấp gói hoặc tạm tắt kênh khác.",
                        activeChannels, plan.getMaxChannel(), plan.getName()
                ));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanEnableChannelAi(Long companyId) {
        Plan plan = getEffectivePlan(companyId);
        if (!Boolean.TRUE.equals(plan.getAiEnabled())) {
            throw new InvalidOperationException(String.format(
                    "Gói dịch vụ %s không hỗ trợ tính năng AI Auto-Reply. Vui lòng nâng cấp gói dịch vụ để kích hoạt AI trên kênh.",
                    plan.getName()
            ));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanAddMember(Long companyId) {
        Plan plan = getEffectivePlan(companyId);
        if (plan.getMaxUser() != null) {
            long activeMembers = companyMemberRepository.countByIdCompanyIdAndStatus(companyId, CompanyMemberStatus.ACTIVE);
            if (activeMembers >= plan.getMaxUser()) {
                throw new InvalidOperationException(String.format(
                        "Công ty đã đạt giới hạn tối đa số nhân sự (%d/%d thành viên) của gói %s. Vui lòng nâng cấp gói dịch vụ để thêm thành viên mới.",
                        activeMembers, plan.getMaxUser(), plan.getName()
                ));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanUploadDocument(Long companyId, long newFileSizeBytes) {
        Plan plan = getEffectivePlan(companyId);
        if (plan.getStorageLimit() != null && plan.getStorageLimit() > 0) {
            Long currentBytes = documentRepository.sumFileSizeByCompanyId(companyId);
            long currentStorageBytes = (currentBytes != null) ? currentBytes : 0L;
            long maxStorageBytes = plan.getStorageLimit() * 1024L * 1024L;

            if (currentStorageBytes + newFileSizeBytes > maxStorageBytes) {
                long currentMb = currentStorageBytes / (1024 * 1024);
                double newFileMb = (double) newFileSizeBytes / (1024.0 * 1024.0);
                throw new InvalidOperationException(String.format(
                        "Không thể tải lên tài liệu (%.2f MB). Tổng dung lượng lưu trữ của công ty sẽ vượt quá hạn mức %d MB của gói %s (Hiện tại đã dùng: %d MB).",
                        newFileMb, plan.getStorageLimit(), plan.getName(), currentMb
                ));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAiQuotaAvailable(Long companyId) {
        Plan plan = getEffectivePlan(companyId);
        if (!Boolean.TRUE.equals(plan.getAiEnabled())) {
            return false;
        }
        if (plan.getAiQuota() == null || plan.getAiQuota() <= 0) {
            return false;
        }

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long usedThisMonth = conversationMessageRepository.countAiMessagesSince(companyId, startOfMonth);
        return usedThisMonth < plan.getAiQuota();
    }

    @Override
    @Transactional(readOnly = true)
    public UsageStatsDto getCompanyUsageStats(Long companyId) {
        Plan plan = getEffectivePlan(companyId);
        long activeUsers = companyMemberRepository.countByIdCompanyIdAndStatus(companyId, CompanyMemberStatus.ACTIVE);
        long activeChannels = channelRepository.countByCompanyIdAndStatus(companyId, ChannelStatus.ACTIVE);
        Long currentBytes = documentRepository.sumFileSizeByCompanyId(companyId);
        long usedStorageBytes = (currentBytes != null) ? currentBytes : 0L;

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long usedAiQuota = conversationMessageRepository.countAiMessagesSince(companyId, startOfMonth);

        UsageStatsDto stats = new UsageStatsDto();
        stats.setMaxUsers(plan.getMaxUser());
        stats.setActiveUsers(activeUsers);
        stats.setMaxChannels(plan.getMaxChannel());
        stats.setActiveChannels(activeChannels);
        stats.setStorageLimitMb(plan.getStorageLimit());
        stats.setUsedStorageBytes(usedStorageBytes);
        stats.setAiEnabled(Boolean.TRUE.equals(plan.getAiEnabled()));
        stats.setAiQuota(plan.getAiQuota());
        stats.setUsedAiQuotaThisMonth(usedAiQuota);

        return stats;
    }
}
