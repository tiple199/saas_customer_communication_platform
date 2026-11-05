package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateKnowledgeBaseRequest {

    @NotBlank(message = "Tên cơ sở tri thức không được để trống")
    @Size(max = 100, message = "Tên cơ sở tri thức không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    public UpdateKnowledgeBaseRequest() {
    }

    public UpdateKnowledgeBaseRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

