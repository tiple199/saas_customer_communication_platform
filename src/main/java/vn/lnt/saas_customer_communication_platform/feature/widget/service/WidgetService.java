package vn.lnt.saas_customer_communication_platform.feature.widget.service;

import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.ConversationMessageResponse;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.RestoreConversationResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.PublicWidgetConfigResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeResponse;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetSendMessageRequest;

public interface WidgetService {

    PublicWidgetConfigResponse getWidgetConfig(String channelKey, String requestOrigin);

    WidgetHandshakeResponse handshake(WidgetHandshakeRequest request, String requestOrigin);

    RestoreConversationResponse restoreConversation(String channelKey, String visitorToken, String requestOrigin);

    ConversationMessageResponse sendMessage(WidgetSendMessageRequest request, String requestOrigin);
}
