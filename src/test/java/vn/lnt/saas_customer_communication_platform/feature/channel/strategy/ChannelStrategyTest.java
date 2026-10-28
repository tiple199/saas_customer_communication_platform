package vn.lnt.saas_customer_communication_platform.feature.channel.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.CreateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.dto.UpdateChannelRequest;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.channel.strategy.impl.TelegramChannelStrategy;
import vn.lnt.saas_customer_communication_platform.feature.channel.strategy.impl.WebChannelStrategy;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChannelStrategyTest {

    private ChannelStrategyFactory strategyFactory;
    private WebChannelStrategy webChannelStrategy;
    private TelegramChannelStrategy telegramChannelStrategy;

    @BeforeEach
    void setUp() {
        webChannelStrategy = new WebChannelStrategy();
        telegramChannelStrategy = new TelegramChannelStrategy();
        strategyFactory = new ChannelStrategyFactory(List.of(webChannelStrategy, telegramChannelStrategy));
    }

    @Test
    void shouldResolveWebStrategy() {
        ChannelStrategy strategy = strategyFactory.getStrategy(ChannelType.WEB);
        assertNotNull(strategy);
        assertEquals(ChannelType.WEB, strategy.getChannelType());
        assertTrue(strategy instanceof WebChannelStrategy);
    }

    @Test
    void shouldResolveTelegramStrategy() {
        ChannelStrategy strategy = strategyFactory.getStrategy(ChannelType.TELEGRAM);
        assertNotNull(strategy);
        assertEquals(ChannelType.TELEGRAM, strategy.getChannelType());
        assertTrue(strategy instanceof TelegramChannelStrategy);
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        assertThrows(InvalidOperationException.class, () -> strategyFactory.getStrategy(null));
    }

    @Test
    void webStrategyAutoGeneratesWidgetKey() {
        Channel channel = new Channel();
        channel.setName("My Web Widget");
        CreateChannelRequest request = new CreateChannelRequest("My Web Widget", ChannelType.WEB, null);

        webChannelStrategy.initChannel(channel, request);

        assertNotNull(channel.getConfig());
        assertTrue(channel.getConfig().contains("widgetKey"));
        assertTrue(channel.getConfig().contains("web_"));
    }

    @Test
    void webStrategyPreservesExistingConfigAndAddsWidgetKey() {
        Channel channel = new Channel();
        channel.setName("Custom Web Widget");
        Map<String, Object> configMap = Map.of("welcomeMessage", "Xin chào quý khách");
        CreateChannelRequest request = new CreateChannelRequest("Custom Web Widget", ChannelType.WEB, configMap);

        webChannelStrategy.initChannel(channel, request);

        assertNotNull(channel.getConfig());
        assertTrue(channel.getConfig().contains("welcomeMessage"));
        assertTrue(channel.getConfig().contains("Xin chào quý khách"));
        assertTrue(channel.getConfig().contains("widgetKey"));
    }

    @Test
    void telegramStrategyHandlesConfig() {
        Channel channel = new Channel();
        channel.setName("Telegram Channel");
        Map<String, Object> configMap = Map.of("botToken", "123456:ABC-DEF");
        CreateChannelRequest request = new CreateChannelRequest("Telegram Channel", ChannelType.TELEGRAM, configMap);

        telegramChannelStrategy.initChannel(channel, request);
        assertNotNull(channel.getConfig());
        assertTrue(channel.getConfig().contains("123456:ABC-DEF"));

        UpdateChannelRequest updateRequest = new UpdateChannelRequest("Telegram Channel", ChannelType.TELEGRAM, ChannelStatus.ACTIVE, Map.of("botToken", "new_token"));
        telegramChannelStrategy.updateChannel(channel, updateRequest);
        assertNotNull(channel.getConfig());
        assertTrue(channel.getConfig().contains("new_token"));
    }
}
