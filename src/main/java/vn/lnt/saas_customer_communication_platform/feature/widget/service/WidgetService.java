package vn.lnt.saas_customer_communication_platform.feature.widget.service;

import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;

public interface WidgetService {

    PublicWidgetConfigResponse getWidgetConfig(String channelKey, String requestOrigin);
}
