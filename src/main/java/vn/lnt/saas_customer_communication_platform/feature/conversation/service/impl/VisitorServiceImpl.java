package vn.lnt.saas_customer_communication_platform.feature.conversation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.VisitorResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.VisitorRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.VisitorService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VisitorServiceImpl implements VisitorService {

    public static final String DEFAULT_VISITOR_NAME = "Khách truy cập";

    private final VisitorRepository visitorRepository;

    public VisitorServiceImpl(VisitorRepository visitorRepository) {
        this.visitorRepository = visitorRepository;
    }

    @Override
    @Transactional
    public Visitor createVisitor(Company company, String name, String email) {
        if (company == null || company.getId() == null) {
            throw new InvalidOperationException("Visitor bắt buộc phải thuộc về một công ty hợp lệ");
        }

        LocalDateTime now = LocalDateTime.now();
        Visitor visitor = new Visitor();
        visitor.setVisitorToken(Visitor.generateVisitorToken());
        visitor.setCompany(company);
        visitor.setLastSeenAt(now);
        visitor.setName(name != null && !name.isBlank() ? name.trim() : DEFAULT_VISITOR_NAME);
        if (email != null && !email.isBlank()) {
            visitor.setEmail(email.trim());
        }

        return visitorRepository.save(visitor);
    }

    @Override
    @Transactional
    public VisitorResolutionResult resolveOrCreateVisitor(Company company, String visitorToken, String name, String email) {
        if (company == null || company.getId() == null) {
            throw new InvalidOperationException("Visitor bắt buộc phải thuộc về một công ty hợp lệ");
        }

        if (visitorToken != null && !visitorToken.isBlank()) {
            Optional<Visitor> existingOpt = visitorRepository.findByVisitorTokenAndCompanyId(
                    visitorToken.trim(),
                    company.getId()
            );

            if (existingOpt.isPresent()) {
                Visitor existing = existingOpt.get();
                existing.setLastSeenAt(LocalDateTime.now());
                if (name != null && !name.isBlank()) {
                    existing.setName(name.trim());
                }
                if (email != null && !email.isBlank()) {
                    existing.setEmail(email.trim());
                }
                Visitor saved = visitorRepository.save(existing);
                return new VisitorResolutionResult(saved, false);
            }
        }

        Visitor created = createVisitor(company, name, email);
        return new VisitorResolutionResult(created, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Visitor getVisitorByToken(Long companyId, String visitorToken) {
        if (companyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }
        if (visitorToken == null || visitorToken.isBlank()) {
            throw new InvalidOperationException("Visitor token không được để trống");
        }
        return visitorRepository.findByVisitorTokenAndCompanyId(visitorToken.trim(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "visitorToken", visitorToken));
    }

    @Override
    @Transactional(readOnly = true)
    public Visitor getVisitorById(Long companyId, Long visitorId) {
        if (companyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }
        return visitorRepository.findByIdAndCompanyId(visitorId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));
    }

    @Override
    @Transactional
    public Visitor updateVisitorInfo(Long companyId, String visitorToken, String name, String email) {
        Visitor visitor = getVisitorByToken(companyId, visitorToken);
        visitor.setLastSeenAt(LocalDateTime.now());
        if (name != null && !name.isBlank()) {
            visitor.setName(name.trim());
        }
        if (email != null && !email.isBlank()) {
            visitor.setEmail(email.trim());
        }
        return visitorRepository.save(visitor);
    }
}

