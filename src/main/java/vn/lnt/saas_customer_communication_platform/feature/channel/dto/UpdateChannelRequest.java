package vn.lnt.saas_customer_communication_platform.feature.channel.dto;

import jakarta.validation.constraints.Size;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;

import java.util.Map;

public class UpdateChannelRequest {

    @Size(max = 100, message = "Tên channel tối đa 100 ký tự")
    private String name;

    private ChannelType type;

    private ChannelStatus status;

    private Map<String, Object> config;

    public UpdateChannelRequest() {
    }

    public UpdateChannelRequest(String name, ChannelType type, ChannelStatus status, Map<String, Object> config) {
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

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public void setStatus(ChannelStatus status) {
        this.status = status;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
