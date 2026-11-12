package vn.lnt.saas_customer_communication_platform.feature.subscription.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.security.CompanySecurityEvaluator;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentGatewayStrategy;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentGatewayStrategyFactory;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentProvider;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PaymentRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    // Thông tin tài khoản thụ hưởng mặc định (VietQR)
    public static final String DEFAULT_BANK_CODE = "MB";
    public static final String DEFAULT_BANK_NAME = "Ngân hàng TMCP Quân đội (MB Bank)";
    public static final String DEFAULT_BANK_ACCOUNT = "0987654321";
    public static final String DEFAULT_ACCOUNT_NAME = "SAAS CUSTOMER COMMUNICATION PLATFORM";

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanySecurityEvaluator companySecurityEvaluator;
    private final PaymentGatewayStrategyFactory paymentGatewayStrategyFactory;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              SubscriptionRepository subscriptionRepository,
                              PlanRepository planRepository,
                              CompanyRepository companyRepository,
                              UserRepository userRepository,
                              CompanySecurityEvaluator companySecurityEvaluator,
                              PaymentGatewayStrategyFactory paymentGatewayStrategyFactory) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.companySecurityEvaluator = companySecurityEvaluator;
        this.paymentGatewayStrategyFactory = paymentGatewayStrategyFactory;
    }

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(String currentUserEmail, Long companyId, CreatePaymentOrderRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Chỉ chủ sở hữu (Owner) hoặc quản trị viên (Admin) của công ty mới có quyền tạo đơn thanh toán");
        }

        Company company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", targetCompanyId));

        Subscription currentSub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElse(null);

        String orderType = request.getOrderType() != null ? request.getOrderType().toUpperCase().trim() : "UPGRADE";
        int durationMonths = (request.getDurationMonths() != null && request.getDurationMonths() == 12) ? 12 : 1;
        String provider = (request.getPaymentProvider() != null && !request.getPaymentProvider().isBlank())
                ? request.getPaymentProvider().toUpperCase().trim() : "VIETQR";

        LocalDateTime now = LocalDateTime.now();
        BigDecimal amountToPay;
        BigDecimal fullPrice;
        BigDecimal unusedCredit = BigDecimal.ZERO;
        long remainingDays = 0;
        Plan targetPlan;

        if ("RENEW".equals(orderType)) {
            if (currentSub == null || currentSub.getPlan() == null || currentSub.getPlan().getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOperationException("Gói cước hiện tại là gói Miễn phí (Starter) nên không cần gia hạn");
            }
            targetPlan = currentSub.getPlan();
            fullPrice = targetPlan.getPrice().multiply(BigDecimal.valueOf(durationMonths));
            amountToPay = fullPrice;
        } else {
            // "UPGRADE"
            if (request.getTargetPlanId() == null) {
                throw new InvalidOperationException("Vui lòng chọn gói cước muốn nâng cấp (targetPlanId)");
            }
            targetPlan = planRepository.findById(request.getTargetPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getTargetPlanId()));

            if (targetPlan.getStatus() != PlanStatus.ACTIVE) {
                throw new InvalidOperationException("Gói dịch vụ được chọn hiện không còn mở đăng ký mới");
            }

            boolean isAnnual = isAnnualSubscription(currentSub, now);
            if (isAnnual && durationMonths != 12) {
                throw new InvalidOperationException("Gói hiện tại của bạn là gói theo Năm. Khi nâng cấp, bạn cần chọn chu kỳ 1 Năm (12 Tháng) để bảo toàn giá trị khấu trừ tiền thừa.");
            }

            fullPrice = targetPlan.getPrice().multiply(BigDecimal.valueOf(durationMonths));

            // Tính Proration tiền thừa
            if (currentSub != null && currentSub.getPlan() != null
                    && currentSub.getPlan().getPrice().compareTo(BigDecimal.ZERO) > 0
                    && currentSub.getEndAt() != null && currentSub.getEndAt().isAfter(now)) {

                Duration durationRemaining = Duration.between(now, currentSub.getEndAt());
                remainingDays = Math.max(1, durationRemaining.toDays());

                BigDecimal dailyRate = currentSub.getPlan().getPrice()
                        .divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);

                unusedCredit = dailyRate.multiply(BigDecimal.valueOf(remainingDays))
                        .setScale(0, RoundingMode.DOWN);

                if (unusedCredit.compareTo(fullPrice) > 0) {
                    unusedCredit = fullPrice;
                }
            }

            amountToPay = fullPrice.subtract(unusedCredit);
            if (amountToPay.compareTo(BigDecimal.ZERO) < 0) {
                amountToPay = BigDecimal.ZERO;
            }
        }

        // Tạo Subscription tạm nếu công ty chưa có bất kỳ subscription nào
        if (currentSub == null) {
            Subscription newSub = new Subscription();
            newSub.setCompany(company);
            newSub.setPlan(targetPlan);
            newSub.setStatus("INACTIVE");
            newSub.setStartAt(now);
            newSub.setEndAt(now);
            currentSub = subscriptionRepository.save(newSub);
        }

        if ("UPGRADE".equals(orderType)) {
            currentSub.setPendingPlanId(targetPlan.getId());
            currentSub = subscriptionRepository.save(currentSub);
        }

        // Sinh orderCode dạng số dương (tương thích PayOS orderCode)
        long orderCode = (System.currentTimeMillis() / 1000L) * 1000L + (long)((Math.random() * 900L) + 100L);

        // Sinh mã giao dịch duy nhất mã hóa cả loại đơn và số tháng
        String prefix = "RENEW".equals(orderType) ? "RNW" : "UPG";
        String durationSuffix = (durationMonths == 12) ? "_12M_" : "_1M_";
        String transactionRef = prefix + durationSuffix + orderCode;

        // Chọn Strategy tương ứng với provider được yêu cầu (hoặc fallback mặc định)
        PaymentProvider requestedProvider = PaymentProvider.fromString(provider);
        PaymentGatewayStrategy strategy = paymentGatewayStrategyFactory.getStrategy(requestedProvider);

        // Gọi Payment Gateway Strategy (PayOS, VietQR, VNPay, Simulated,...)
        PaymentGatewayOrderRequest gwReq = new PaymentGatewayOrderRequest();
        gwReq.setOrderCode(orderCode);
        gwReq.setAmount(amountToPay);
        gwReq.setDescription(prefix + " " + targetPlan.getName());
        gwReq.setOrderType(orderType);
        gwReq.setTargetPlanName(targetPlan.getName());

        PaymentGatewayOrderResponse gwResp = strategy.createPaymentOrder(gwReq);

        Payment payment = new Payment();
        payment.setSubscription(currentSub);
        payment.setAmount(amountToPay);
        payment.setCurrency("VND");
        payment.setProvider(strategy.getProvider().name());
        payment.setStatus("PENDING");
        payment.setTransactionRef(transactionRef);

        // Nếu số tiền phải trả là 0 VND (do tiền thừa khấu trừ 100%) hoặc chọn phương thức thử nghiệm kích hoạt ngay (SIMULATED)
        if (amountToPay.compareTo(BigDecimal.ZERO) == 0 || strategy.getProvider() == PaymentProvider.SIMULATED) {
            payment.setStatus("SUCCESS");
            payment.setPaidAt(now);
            payment = paymentRepository.save(payment);
            executePaymentSuccess(payment, now);
        } else {
            payment = paymentRepository.save(payment);
        }

        PaymentOrderResponse resp = new PaymentOrderResponse();
        resp.setPaymentId(payment.getId());
        resp.setTransactionRef(payment.getTransactionRef());
        resp.setOrderType(orderType);
        resp.setAmount(amountToPay);
        resp.setFullPrice(fullPrice);
        resp.setUnusedCreditAmount(unusedCredit);
        resp.setRemainingDaysCredited(remainingDays);
        resp.setCurrency(payment.getCurrency());
        resp.setStatus(payment.getStatus());
        resp.setTargetPlanId(targetPlan.getId());
        resp.setTargetPlanName(targetPlan.getName());
        resp.setDurationMonths(durationMonths);
        resp.setProvider(payment.getProvider());
        resp.setQrCodeUrl(gwResp.getQrCodeUrl());
        resp.setCheckoutUrl(gwResp.getCheckoutUrl());
        resp.setBankCode(gwResp.getBankCode());
        resp.setBankName(gwResp.getBankName());
        resp.setBankAccountNumber(gwResp.getAccountNumber());
        resp.setBankAccountName(gwResp.getAccountName());
        resp.setTransferContent(transactionRef);
        resp.setCreatedAt(payment.getCreatedAt());
        resp.setExpiresAt(payment.getCreatedAt() != null ? payment.getCreatedAt().plusMinutes(15) : now.plusMinutes(15));

        if ("SUCCESS".equals(payment.getStatus())) {
            if (strategy.getProvider() == PaymentProvider.SIMULATED) {
                resp.setMessage("Thanh toán thử nghiệm thành công! Gói cước đã được kích hoạt ngay lập tức.");
            } else {
                resp.setMessage("Giao dịch đã được kích hoạt thành công (Khấu trừ tiền thừa 100%).");
            }
        } else {
            resp.setMessage("Đơn thanh toán đã được khởi tạo. Vui lòng quét mã QR hoặc hoàn tất thanh toán qua PayOS.");
        }

        return resp;
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String currentUserEmail, ConfirmPaymentRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);

        Payment payment = paymentRepository.findByTransactionRef(request.getTransactionRef())
                .or(() -> paymentRepository.findFirstByTransactionRefContaining(request.getTransactionRef()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionRef", request.getTransactionRef()));

        Long companyId = payment.getSubscription() != null && payment.getSubscription().getCompany() != null
                ? payment.getSubscription().getCompany().getId() : null;

        if (companyId != null && !isSuperAdminRole(currentUser.getRole())
                && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), companyId)) {
            throw new InvalidOperationException("Bạn không có quyền xác nhận đơn thanh toán này");
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            return PaymentResponse.fromEntity(payment);
        }

        if ("CANCELLED".equals(payment.getStatus())) {
            throw new InvalidOperationException("Đơn thanh toán này đã bị hủy bỏ");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus("SUCCESS");
        payment.setPaidAt(now);
        payment = paymentRepository.save(payment);

        executePaymentSuccess(payment, now);
        log.info("[Payment] Đã xác nhận thành công đơn giao dịch {} cho công ty #{}", payment.getTransactionRef(), companyId);

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public PaymentResponse handlePaymentWebhook(PaymentWebhookRequest request) {
        if (request.getTransactionRef() == null || request.getTransactionRef().isBlank()) {
            throw new InvalidOperationException("Mã giao dịch transactionRef không hợp lệ");
        }

        Payment payment = paymentRepository.findByTransactionRef(request.getTransactionRef())
                .or(() -> paymentRepository.findFirstByTransactionRefContaining(request.getTransactionRef()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionRef", request.getTransactionRef()));

        if ("SUCCESS".equals(payment.getStatus())) {
            return PaymentResponse.fromEntity(payment);
        }

        LocalDateTime now = LocalDateTime.now();
        if ("FAILED".equalsIgnoreCase(request.getStatus())) {
            payment.setStatus("FAILED");
            payment = paymentRepository.save(payment);
            return PaymentResponse.fromEntity(payment);
        }

        // Webhook SUCCESS
        payment.setStatus("SUCCESS");
        payment.setPaidAt(now);
        if (request.getProvider() != null) {
            payment.setProvider(request.getProvider());
        }
        payment = paymentRepository.save(payment);

        executePaymentSuccess(payment, now);
        log.info("[Payment Webhook] Đã xử lý Webhook thành công đơn giao dịch {}", payment.getTransactionRef());

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public PaymentResponse handlePayOSWebhook(PayOSWebhookPayload payload) {
        if (payload == null || payload.getData() == null || payload.getData().getOrderCode() == null) {
            throw new InvalidOperationException("Payload Webhook từ PayOS không hợp lệ");
        }

        PaymentGatewayStrategy payOsStrategy = paymentGatewayStrategyFactory.getStrategy(PaymentProvider.PAYOS);
        if (payload.getSignature() != null && payOsStrategy.isConfigured()) {
            boolean valid = payOsStrategy.verifyWebhookSignature(payload.getSignatureData(), payload.getSignature());
            if (!valid) {
                log.warn("[PayOS Webhook] Chữ ký webhook PayOS không hợp lệ cho orderCode={}", payload.getData().getOrderCode());
                throw new InvalidOperationException("Chữ ký webhook PayOS không hợp lệ");
            }
        }

        Long orderCode = payload.getData().getOrderCode();
        Payment payment = paymentRepository.findFirstByTransactionRefContaining(String.valueOf(orderCode))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderCode", orderCode));

        if ("SUCCESS".equals(payment.getStatus())) {
            return PaymentResponse.fromEntity(payment);
        }

        LocalDateTime now = LocalDateTime.now();
        if ("00".equals(payload.getCode())) {
            payment.setStatus("SUCCESS");
            payment.setPaidAt(now);
            payment.setProvider("PAYOS");
            payment = paymentRepository.save(payment);

            executePaymentSuccess(payment, now);
            log.info("[PayOS Webhook] Giao dịch PayOS #{} (orderCode={}) đã được kích hoạt thành công",
                    payment.getTransactionRef(), orderCode);
        } else {
            payment.setStatus("FAILED");
            payment = paymentRepository.save(payment);
            log.warn("[PayOS Webhook] Giao dịch PayOS #{} thất bại với code: {}", payment.getTransactionRef(), payload.getCode());
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPaymentOrder(String currentUserEmail, String transactionRef) {
        User currentUser = getUserByEmail(currentUserEmail);

        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .or(() -> paymentRepository.findFirstByTransactionRefContaining(transactionRef))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionRef", transactionRef));

        Long companyId = payment.getSubscription() != null && payment.getSubscription().getCompany() != null
                ? payment.getSubscription().getCompany().getId() : null;

        if (companyId != null && !isSuperAdminRole(currentUser.getRole())
                && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), companyId)) {
            throw new InvalidOperationException("Bạn không có quyền hủy đơn thanh toán này");
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new InvalidOperationException("Không thể hủy giao dịch đã thanh toán thành công");
        }

        payment.setStatus("CANCELLED");
        payment = paymentRepository.save(payment);

        Subscription sub = payment.getSubscription();
        if (sub != null && sub.getPendingPlanId() != null && payment.getTransactionRef().startsWith("UPG")) {
            sub.setPendingPlanId(null);
            subscriptionRepository.save(sub);
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(String currentUserEmail, String transactionRef) {
        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .or(() -> paymentRepository.findFirstByTransactionRefContaining(transactionRef))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionRef", transactionRef));
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getCompanyPayments(String currentUserEmail, Long companyId, Pageable pageable) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isMember(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Bạn không có quyền truy cập lịch sử thanh toán của công ty này");
        }

        int validPage = Math.max(pageable.getPageNumber(), 0);
        int validSize = pageable.getPageSize() <= 0 ? 10 : Math.min(pageable.getPageSize(), 50);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(validPage, validSize, sort);

        Page<Payment> page = paymentRepository.findBySubscriptionCompanyIdOrderByCreatedAtDesc(targetCompanyId, safePageable);
        java.util.List<PaymentResponse> items = page.getContent().stream()
                .map(PaymentResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String currentUserEmail, Long paymentId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        Long companyId = payment.getSubscription() != null && payment.getSubscription().getCompany() != null
                ? payment.getSubscription().getCompany().getId() : null;

        if (companyId != null && !isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isMember(currentUser.getId(), companyId)) {
            throw new InvalidOperationException("Bạn không có quyền truy cập chi tiết giao dịch này");
        }

        return PaymentResponse.fromEntity(payment);
    }

    private void executePaymentSuccess(Payment payment, LocalDateTime now) {
        Subscription sub = payment.getSubscription();
        if (sub == null) {
            return;
        }

        String ref = payment.getTransactionRef() != null ? payment.getTransactionRef() : "";
        boolean isRenew = ref.startsWith("RNW");
        int months = ref.contains("_12M_") ? 12 : 1;

        if (isRenew) {
            // Gia hạn: Cộng thêm số tháng vào endAt hiện tại
            LocalDateTime previousEndAt = sub.getEndAt();
            LocalDateTime baseDate = (previousEndAt != null && previousEndAt.isAfter(now)) ? previousEndAt : now;
            LocalDateTime newEndAt = baseDate.plusMonths(months);

            sub.setStatus("ACTIVE");
            sub.setEndAt(newEndAt);
            sub.setPendingPlanId(null);
            subscriptionRepository.save(sub);
            log.info("[Payment Success] Đã gia hạn thành công Subscription #{} đến ngày {}", sub.getId(), newEndAt);
        } else {
            // Nâng cấp gói cước UPGRADE: lấy từ pendingPlanId của Subscription
            Long targetPlanId = sub.getPendingPlanId();
            Plan targetPlan = null;
            if (targetPlanId != null) {
                targetPlan = planRepository.findById(targetPlanId).orElse(null);
            }
            if (targetPlan != null) {
                sub.setPlan(targetPlan);
                sub.setStatus("ACTIVE");
                sub.setStartAt(now);
                sub.setEndAt(now.plusMonths(months));
                sub.setPendingPlanId(null);
                subscriptionRepository.save(sub);
                log.info("[Payment Success] Đã nâng cấp thành công Subscription #{} sang gói {} đến ngày {}",
                        sub.getId(), targetPlan.getName(), sub.getEndAt());
            }
        }
    }

    private boolean isAnnualSubscription(Subscription currentSub, LocalDateTime now) {
        if (currentSub == null || currentSub.getEndAt() == null || currentSub.getEndAt().isBefore(now)) {
            return false;
        }
        if (currentSub.getPlan() == null || currentSub.getPlan().getPrice() == null
                || currentSub.getPlan().getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (currentSub.getStartAt() != null && Duration.between(currentSub.getStartAt(), currentSub.getEndAt()).toDays() > 60) {
            return true;
        }
        return Duration.between(now, currentSub.getEndAt()).toDays() > 30;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private boolean isSuperAdminRole(String role) {
        return "ROLE_SUPER_ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
    }
}
