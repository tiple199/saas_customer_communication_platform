package vn.lnt.saas_customer_communication_platform.feature.channel.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.ChannelResponse;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;

import java.util.List;

public interface ChannelService {

    ChannelResponse createChannel(String currentUserEmail, Long companyId, CreateChannelRequest request);

    List<ChannelResponse> getChannels(String currentUserEmail, Long companyId);

    PageResponse<ChannelResponse> getChannelsPaged(String currentUserEmail, Long companyId, Pageable pageable);

    ChannelResponse getChannelById(String currentUserEmail, Long companyId, Long channelId);

    ChannelResponse updateChannel(String currentUserEmail, Long companyId, Long channelId, UpdateChannelRequest request);

    void deleteChannel(String currentUserEmail, Long companyId, Long channelId);
}
