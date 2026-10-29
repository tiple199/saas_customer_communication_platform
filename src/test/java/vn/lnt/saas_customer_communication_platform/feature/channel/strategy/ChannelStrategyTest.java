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
    void webStrategyUsesChannelKeyWhenAvailable() {
        Channel channel = new Channel();
        channel.setName("My Web Widget");
        channel.setChannelKey("chk_custom_key_123");
        CreateChannelRequest request = new CreateChannelRequest("My Web Widget", ChannelType.WEB, null);

        webChannelStrategy.initChannel(channel, request);

        assertNotNull(channel.getConfig());
        assertTrue(channel.getConfig().contains("chk_custom_key_123"));
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

    @Test
    void telegramStrategyThrowsExceptionWhenBotTokenMissing() {
        Channel channel = new Channel();
        channel.setName("Telegram Channel");
        CreateChannelRequest request = new CreateChannelRequest("Telegram Channel", ChannelType.TELEGRAM, Map.of());

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> telegramChannelStrategy.initChannel(channel, request));
        assertTrue(ex.getMessage().contains("Bot token của Telegram là bắt buộc"));
    }

    @Test
    void telegramStrategyThrowsExceptionWhenBotTokenIsBlank() {
        Channel channel = new Channel();
        channel.setName("Telegram Channel");
        CreateChannelRequest request = new CreateChannelRequest("Telegram Channel", ChannelType.TELEGRAM, Map.of("botToken", "   "));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> telegramChannelStrategy.initChannel(channel, request));
        assertTrue(ex.getMessage().contains("Bot token của Telegram là bắt buộc"));
    }

    @Test
    void webStrategyThrowsExceptionWhenWelcomeMessageTooLong() {
        Channel channel = new Channel();
        channel.setName("Web Channel");
        String longMessage = "a".repeat(501);
        CreateChannelRequest request = new CreateChannelRequest("Web Channel", ChannelType.WEB, Map.of("welcomeMessage", longMessage));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> webChannelStrategy.initChannel(channel, request));
        assertTrue(ex.getMessage().contains("Lời chào không được vượt quá 500 ký tự"));
    }

    @Test
    void webStrategyNormalizesAllowedDomains() {
        Channel channel = new Channel();
        channel.setName("Web Channel");
        CreateChannelRequest request = new CreateChannelRequest("Web Channel", ChannelType.WEB,
                Map.of("allowedDomains", " shop.my-domain.vn , localhost:3000 "));

        webChannelStrategy.initChannel(channel, request);

        assertNotNull(channel.getConfig());
        assertTrue(channel.getConfig().contains("shop.my-domain.vn,localhost:3000"));
    }

    @Test
    void webStrategyThrowsExceptionWhenAllowedDomainHasProtocol() {
        Channel channel = new Channel();
        channel.setName("Web Channel");
        CreateChannelRequest request = new CreateChannelRequest("Web Channel", ChannelType.WEB,
                Map.of("allowedDomains", "https://shop.my-domain.vn"));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> webChannelStrategy.initChannel(channel, request));
        assertTrue(ex.getMessage().contains("Không nhập giao thức (http/https) hoặc đường dẫn (/)"));
    }

    @Test
    void webStrategyThrowsExceptionWhenAllowedDomainIsInvalid() {
        Channel channel = new Channel();
        channel.setName("Web Channel");
        CreateChannelRequest request = new CreateChannelRequest("Web Channel", ChannelType.WEB,
                Map.of("allowedDomains", "invalid domain with space.com"));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> webChannelStrategy.initChannel(channel, request));
        assertTrue(ex.getMessage().contains("Tên miền không hợp lệ"));
    }
}
