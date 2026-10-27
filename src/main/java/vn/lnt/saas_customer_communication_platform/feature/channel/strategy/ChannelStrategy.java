package vn.lnt.saas_customer_communication_platform.feature.channel.strategy;

import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;

/**
 * Strategy xử lý logic cấu hình chuyên biệt cho từng loại kênh kết nối.
 */
public interface ChannelStrategy {

    /**
     * Xác định loại kênh kết nối (WEB, TELEGRAM, ...).
     */
    ChannelType getChannelType();

    /**
     * Khởi tạo cấu hình và thiết lập ban đầu cho kênh khi tạo mới.
     */
    void initChannel(Channel channel, CreateChannelRequest request);

    /**
     * Cập nhật cấu hình cho kênh khi chỉnh sửa.
     */
    void updateChannel(Channel channel, UpdateChannelRequest request);
}
