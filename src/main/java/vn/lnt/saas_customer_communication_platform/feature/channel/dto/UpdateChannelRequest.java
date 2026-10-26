package vn.lnt.saas_customer_communication_platform.feature.channel.dto;

import jakarta.validation.constraints.Size;

public class UpdateChannelRequest {

    @Size(max = 100, message = "Tên channel tối đa 100 ký tự")
    private String name;

    private String type;

    private String status;

    private String config;

    public UpdateChannelRequest() {
    }

    public UpdateChannelRequest(String name, String type, String status, String config) {
        this.name = name;
        this.type = type;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
