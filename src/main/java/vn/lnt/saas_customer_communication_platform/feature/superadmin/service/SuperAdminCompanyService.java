package vn.lnt.saas_customer_communication_platform.feature.superadmin.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminAssignPlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminCompanyDetailDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminCompanyItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.UpdateCompanyStatusRequest;

public interface SuperAdminCompanyService {

    PageResponse<SuperAdminCompanyItemDto> getCompanies(String currentUserEmail,
                                                        String keyword,
                                                        String status,
                                                        Long planId,
                                                        Pageable pageable);

    SuperAdminCompanyDetailDto getCompanyDetail(String currentUserEmail, Long companyId);

    SuperAdminCompanyItemDto updateCompanyStatus(String currentUserEmail,
                                                 Long companyId,
                                                 UpdateCompanyStatusRequest request);

    SuperAdminCompanyDetailDto assignPlan(String currentUserEmail,
                                          Long companyId,
                                          SuperAdminAssignPlanRequest request);
}

