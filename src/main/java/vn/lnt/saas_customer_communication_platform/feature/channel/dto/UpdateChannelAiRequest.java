package vn.lnt.saas_customer_communication_platform.feature.channel.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateChannelAiRequest {

    @NotNull(message = "Trạng thái AI không được để trống")
    private Boolean enabled;

    public UpdateChannelAiRequest() {
    }

    public UpdateChannelAiRequest(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
