package vn.lnt.saas_customer_communication_platform.feature.superadmin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PaymentRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminDashboardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class SuperAdminDashboardServiceImpl implements SuperAdminDashboardService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminDashboardServiceImpl.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final ConversationRepository conversationRepository;
    private final ChannelRepository channelRepository;

    public SuperAdminDashboardServiceImpl(UserRepository userRepository,
                                          CompanyRepository companyRepository,
                                          CompanyMemberRepository companyMemberRepository,
                                          SubscriptionRepository subscriptionRepository,
                                          PaymentRepository paymentRepository,
                                          ConversationRepository conversationRepository,
                                          ChannelRepository channelRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getDashboardStats(String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        if (!isSuperAdminRole(currentUser.getRole())) {
            throw new InvalidOperationException("Chỉ Super Admin mới có quyền truy cập bảng điều khiển hệ thống");
        }

        LocalDateTime now = LocalDateTime.now();
        YearMonth currentYearMonth = YearMonth.from(now);
        LocalDateTime startOfCurrentMonth = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime sixMonthsAgo = currentYearMonth.minusMonths(5).atDay(1).atStartOfDay();

        SuperAdminDashboardResponse resp = new SuperAdminDashboardResponse();

        // 1. Overview KPIs
        long totalCompanies = companyRepository.count();
        long newCompaniesThisMonth = companyRepository.countNewCompaniesSince(startOfCurrentMonth);
        long totalUsers = userRepository.count();
        long activeSubscriptions = subscriptionRepository.countByStatus("ACTIVE");
        BigDecimal totalRevenue = paymentRepository.calculateTotalRevenue();
        BigDecimal revenueThisMonth = paymentRepository.calculateRevenueSince(startOfCurrentMonth);
        long totalConversations = conversationRepository.count();
        long totalChannels = channelRepository.count();

        resp.setTotalCompanies(totalCompanies);
        resp.setNewCompaniesThisMonth(newCompaniesThisMonth);
        resp.setTotalUsers(totalUsers);
        resp.setActiveSubscriptions(activeSubscriptions);
        resp.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        resp.setRevenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO);
        resp.setTotalConversations(totalConversations);
        resp.setTotalChannels(totalChannels);

        // 2. Plan Distribution
        List<Object[]> rawPlanDist = subscriptionRepository.getActivePlanDistribution();
        List<PlanDistributionDto> planDistributions = new ArrayList<>();
        long totalActiveSubs = Math.max(1, activeSubscriptions);

        for (Object[] row : rawPlanDist) {
            Long planId = ((Number) row[0]).longValue();
            String planName = (String) row[1];
            BigDecimal price = (BigDecimal) row[2];
            long count = ((Number) row[3]).longValue();
            double pct = BigDecimal.valueOf((double) count * 100.0 / totalActiveSubs)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();

            planDistributions.add(new PlanDistributionDto(planId, planName, price, count, pct));
        }
        resp.setPlanDistributions(planDistributions);

        // 3. Monthly Trend (6 tháng gần nhất)
        List<Payment> recentSuccessPayments = paymentRepository.findSuccessfulPaymentsSince(sixMonthsAgo);
        List<Company> recentCreatedCompanies = companyRepository.findCompaniesCreatedSince(sixMonthsAgo);
        List<MonthlyTrendDto> monthlyTrends = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentYearMonth.minusMonths(i);
            LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = ym.atEndOfMonth().atTime(LocalTime.MAX);

            BigDecimal monthRevenue = BigDecimal.ZERO;
            long txCount = 0;
            for (Payment p : recentSuccessPayments) {
                if (p.getPaidAt() != null && !p.getPaidAt().isBefore(monthStart) && !p.getPaidAt().isAfter(monthEnd)) {
                    monthRevenue = monthRevenue.add(p.getAmount());
                    txCount++;
                }
            }

            long newCompanies = 0;
            for (Company c : recentCreatedCompanies) {
                if (c.getCreatedAt() != null && !c.getCreatedAt().isBefore(monthStart) && !c.getCreatedAt().isAfter(monthEnd)) {
                    newCompanies++;
                }
            }

            String monthKey = ym.toString();
            String monthLabel = "T" + ym.getMonthValue() + "/" + ym.getYear();
            monthlyTrends.add(new MonthlyTrendDto(monthKey, monthLabel, monthRevenue, txCount, newCompanies));
        }
        resp.setMonthlyTrends(monthlyTrends);

        // 4. Top 10 Recent Payments
        List<Payment> topPayments = paymentRepository.findTopRecentPayments(PageRequest.of(0, 10));
        List<RecentPaymentDto> recentPayments = new ArrayList<>();
        for (Payment p : topPayments) {
            RecentPaymentDto dto = new RecentPaymentDto();
            dto.setId(p.getId());
            dto.setTransactionRef(p.getTransactionRef());
            dto.setAmount(p.getAmount());
            dto.setCurrency(p.getCurrency());
            dto.setProvider(p.getProvider());
            dto.setStatus(p.getStatus());
            dto.setCreatedAt(p.getCreatedAt());
            dto.setPaidAt(p.getPaidAt());

            if (p.getSubscription() != null) {
                if (p.getSubscription().getCompany() != null) {
                    dto.setCompanyId(p.getSubscription().getCompany().getId());
                    dto.setCompanyName(p.getSubscription().getCompany().getName());
                }
                if (p.getSubscription().getPlan() != null) {
                    dto.setPlanName(p.getSubscription().getPlan().getName());
                }
            }
            recentPayments.add(dto);
        }
        resp.setRecentPayments(recentPayments);

        // 5. Top 10 Recent Companies
        List<Company> topCompanies = companyRepository.findTopRecentCompanies(PageRequest.of(0, 10));
        List<RecentCompanyDto> recentCompanies = new ArrayList<>();
        for (Company c : topCompanies) {
            RecentCompanyDto dto = new RecentCompanyDto();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setLogoUrl(c.getLogoUrl());
            dto.setStatus(c.getStatus());
            dto.setCreatedAt(c.getCreatedAt());

            long members = companyMemberRepository.countByIdCompanyIdAndStatus(c.getId(), CompanyMemberStatus.ACTIVE);
            dto.setMemberCount(members);

            Subscription sub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(c.getId(), "ACTIVE")
                    .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(c.getId()))
                    .orElse(null);

            if (sub != null && sub.getPlan() != null) {
                dto.setCurrentPlanName(sub.getPlan().getName());
            } else {
                dto.setCurrentPlanName("Chưa kích hoạt");
            }

            recentCompanies.add(dto);
        }
        resp.setRecentCompanies(recentCompanies);

        return resp;
    }

    private boolean isSuperAdminRole(String role) {
        if (role == null) return false;
        String normalized = role.trim().toUpperCase();
        return "ROLE_SUPER_ADMIN".equals(normalized) || "SUPER_ADMIN".equals(normalized);
    }
}

