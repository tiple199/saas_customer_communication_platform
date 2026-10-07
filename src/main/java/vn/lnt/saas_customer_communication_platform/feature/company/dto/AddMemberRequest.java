package vn.lnt.saas_customer_communication_platform.feature.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;

public class AddMemberRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Vai trò không được để trống")
    private CompanyMemberRole role;

    public AddMemberRequest() {
    }

    public AddMemberRequest(String email, CompanyMemberRole role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CompanyMemberRole getRole() {
        return role;
    }

    public void setRole(CompanyMemberRole role) {
        this.role = role;
    }
}
