package vn.lnt.saas_customer_communication_platform.feature.widget.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WidgetHandshakeRequest {

    @NotBlank(message = "Khóa kênh (channelKey) không được để trống")
    private String channelKey;

    @Size(max = 64, message = "Visitor token tối đa 64 ký tự")
    private String visitorToken;

    @Size(max = 100, message = "Tên khách truy cập tối đa 100 ký tự")
    private String name;

    @Email(message = "Email khách truy cập không hợp lệ")
    @Size(max = 150, message = "Email khách truy cập tối đa 150 ký tự")
    private String email;

    public WidgetHandshakeRequest() {
    }

    public WidgetHandshakeRequest(String channelKey, String visitorToken, String name, String email) {
        this.channelKey = channelKey;
        this.visitorToken = visitorToken;
        this.name = name;
        this.email = email;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public String getVisitorToken() {
        return visitorToken;
    }

    public void setVisitorToken(String visitorToken) {
        this.visitorToken = visitorToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

