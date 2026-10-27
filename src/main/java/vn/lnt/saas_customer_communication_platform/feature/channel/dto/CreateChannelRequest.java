package vn.lnt.saas_customer_communication_platform.feature.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;

import java.util.Map;

public class CreateChannelRequest {

    @NotBlank(message = "Tên channel không được để trống")
    @Size(max = 100, message = "Tên channel tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Loại channel không được để trống (WEB hoặc TELEGRAM)")
    private ChannelType type;

    private Map<String, Object> config;

    public CreateChannelRequest() {
    }

    public CreateChannelRequest(String name, ChannelType type, Map<String, Object> config) {
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

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
