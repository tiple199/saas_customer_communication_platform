package vn.lnt.saas_customer_communication_platform.feature.widget.service;

import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeResponse;

public interface WidgetService {

    PublicWidgetConfigResponse getWidgetConfig(String channelKey, String requestOrigin);

    WidgetHandshakeResponse handshake(WidgetHandshakeRequest request, String requestOrigin);
}
