package vn.lnt.saas_customer_communication_platform.feature.conversation.service;

import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.VisitorResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;

public interface VisitorService {

    /**
     * Tạo mới một Visitor gắn với công ty.
     */
    Visitor createVisitor(Company company, String name, String email);

    /**
     * Khôi phục Visitor hiện có theo visitorToken trong phạm vi công ty,
     * hoặc tạo mới nếu chưa có token / token không tồn tại.
     */
    VisitorResolutionResult resolveOrCreateVisitor(Company company, String visitorToken, String name, String email);

    /**
     * Tìm Visitor theo visitorToken và companyId.
     */
    Visitor getVisitorByToken(Long companyId, String visitorToken);

    /**
     * Tìm Visitor theo id và companyId.
     */
    Visitor getVisitorById(Long companyId, Long visitorId);

    /**
     * Cập nhật tên hoặc email của Visitor.
     */
    Visitor updateVisitorInfo(Long companyId, String visitorToken, String name, String email);
}

