package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import jakarta.validation.constraints.NotNull;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.HandlingMode;

public class UpdateHandlingModeRequest {

    @NotNull(message = "Chế độ xử lý (handlingMode) không được để trống")
    private HandlingMode handlingMode;

    public UpdateHandlingModeRequest() {
    }

    public UpdateHandlingModeRequest(HandlingMode handlingMode) {
        this.handlingMode = handlingMode;
    }

    public HandlingMode getHandlingMode() {
        return handlingMode;
    }

    public void setHandlingMode(HandlingMode handlingMode) {
        this.handlingMode = handlingMode;
    }
}

