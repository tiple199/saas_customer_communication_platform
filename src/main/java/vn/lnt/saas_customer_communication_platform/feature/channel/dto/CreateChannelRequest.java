package vn.lnt.saas_customer_communication_platform.feature.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateChannelRequest {

    @NotBlank(message = "Tên channel không được để trống")
    @Size(max = 100, message = "Tên channel tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Loại channel không được để trống (ví dụ: WEB, FACEBOOK, ZALO, TELEGRAM...)")
    private String type;

    private String config;

    public CreateChannelRequest() {
    }

    public CreateChannelRequest(String name, String type, String config) {
        this.name = name;
        this.type = type;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
