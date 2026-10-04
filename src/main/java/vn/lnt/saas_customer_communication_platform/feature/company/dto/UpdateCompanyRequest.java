package vn.lnt.saas_customer_communication_platform.feature.company.dto;

import jakarta.validation.constraints.Size;

public class UpdateCompanyRequest {

    @Size(max = 255, message = "Tên công ty không được vượt quá 255 ký tự")
    private String name;

    private String logoUrl;

    private String status;

    public UpdateCompanyRequest() {
    }

    public UpdateCompanyRequest(String name, String logoUrl, String status) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
