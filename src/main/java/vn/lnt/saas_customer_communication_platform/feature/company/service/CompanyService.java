package vn.lnt.saas_customer_communication_platform.feature.company.service;

import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.UpdateCompanyRequest;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(String currentUserEmail, CreateCompanyRequest request);

    List<CompanyResponse> getMyCompanies(String currentUserEmail);

    CompanyResponse getCompanyById(String currentUserEmail, Long companyId);

    CompanyResponse updateCompany(String currentUserEmail, Long companyId, UpdateCompanyRequest request);

    CompanyResponse switchCompany(String currentUserEmail, Long companyId);

    List<CompanyMemberResponse> getCompanyMembers(String currentUserEmail, Long companyId);
}
