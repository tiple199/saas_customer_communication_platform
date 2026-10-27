package vn.lnt.saas_customer_communication_platform.feature.channel.strategy;

import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory quản lý và phân phối Strategy tương ứng cho từng ChannelType.
 * Tự động nhận diện tất cả các bean implement ChannelStrategy được nạp bởi Spring context.
 */
@Component
public class ChannelStrategyFactory {

    private final Map<ChannelType, ChannelStrategy> strategyMap = new EnumMap<>(ChannelType.class);

    public ChannelStrategyFactory(List<ChannelStrategy> strategies) {
        for (ChannelStrategy strategy : strategies) {
            strategyMap.put(strategy.getChannelType(), strategy);
        }
    }

    /**
     * Lấy ChannelStrategy tương ứng với ChannelType.
     * Ném InvalidOperationException nếu chưa có Strategy xử lý cho loại kênh này.
     */
    public ChannelStrategy getStrategy(ChannelType type) {
        if (type == null) {
            throw new InvalidOperationException("Loại kênh kết nối không được để trống");
        }
        ChannelStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new InvalidOperationException("Chưa hỗ trợ xử lý cho loại kênh kết nối: " + type);
        }
        return strategy;
    }
}
