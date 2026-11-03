package vn.lnt.saas_customer_communication_platform.feature.widget.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelType;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.service.CompanyService;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationMessageRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetSendMessageRequest;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PublicWidgetControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    private String validChannelKey;
    private String restrictedChannelKey;
    private String inactiveChannelKey;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (!userRepository.existsByEmail("widgetowner@example.com")) {
            User user = new User("widgetowner@example.com", "password123", "Widget Owner", "ROLE_USER");
            userRepository.save(user);
        }

        CompanyResponse companyResp = companyService.createCompany("widgetowner@example.com", new CreateCompanyRequest("Widget Corp"));
        Company company = companyRepository.findById(companyResp.getId()).orElseThrow();

        // 1. Channel mở không giới hạn domain (* hoặc trống)
        Channel openChannel = new Channel();
        openChannel.setChannelKey(Channel.generateChannelKey());
        openChannel.setName("Open Web Channel");
        openChannel.setType(ChannelType.WEB);
        openChannel.setStatus(ChannelStatus.ACTIVE);
        openChannel.setAiEnabled(true);
        openChannel.setConfig("{\"welcomeMessage\":\"Chào mừng bạn!\",\"allowedDomains\":\"*\"}");
        openChannel.setCompany(company);
        channelRepository.save(openChannel);
        this.validChannelKey = openChannel.getChannelKey();

        // 2. Channel giới hạn domain: chỉ cho phép shop.example.com và localhost:3000
        Channel restrictedChannel = new Channel();
        restrictedChannel.setChannelKey(Channel.generateChannelKey());
        restrictedChannel.setName("Restricted Web Channel");
        restrictedChannel.setType(ChannelType.WEB);
        restrictedChannel.setStatus(ChannelStatus.ACTIVE);
        restrictedChannel.setAiEnabled(false);
        restrictedChannel.setConfig("{\"welcomeMessage\":\"Hỗ trợ khách hàng\",\"allowedDomains\":\"shop.example.com,localhost:3000\"}");
        restrictedChannel.setCompany(company);
        channelRepository.save(restrictedChannel);
        this.restrictedChannelKey = restrictedChannel.getChannelKey();

        // 3. Channel bị INACTIVE
        Channel inactiveChannel = new Channel();
        inactiveChannel.setChannelKey(Channel.generateChannelKey());
        inactiveChannel.setName("Inactive Channel");
        inactiveChannel.setType(ChannelType.WEB);
        inactiveChannel.setStatus(ChannelStatus.INACTIVE);
        inactiveChannel.setCompany(company);
        channelRepository.save(inactiveChannel);
        this.inactiveChannelKey = inactiveChannel.getChannelKey();
    }

    @Test
    void getWidgetConfigWithoutAuthSuccessTest() throws Exception {
        // Gọi public không cần JWT token
        mockMvc.perform(get("/api/v1/public/widget/config")
                        .param("channelKey", validChannelKey)
                        .header("Origin", "https://anywebsite.vn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.channelKey").value(validChannelKey))
                .andExpect(jsonPath("$.data.name").value("Open Web Channel"))
                .andExpect(jsonPath("$.data.welcomeMessage").value("Chào mừng bạn!"))
                .andExpect(jsonPath("$.data.aiEnabled").value(true));
    }

    @Test
    void getWidgetConfigRestrictedDomainSuccessWhenOriginMatchesTest() throws Exception {
        mockMvc.perform(get("/api/v1/public/widget/config")
                        .param("channelKey", restrictedChannelKey)
                        .header("Origin", "https://shop.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelKey").value(restrictedChannelKey))
                .andExpect(jsonPath("$.data.aiEnabled").value(false));

        // Kiểm tra với Referer header
        mockMvc.perform(get("/api/v1/public/widget/config")
                        .param("channelKey", restrictedChannelKey)
                        .header("Referer", "http://localhost:3000/product/456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelKey").value(restrictedChannelKey));
    }

    @Test
    void getWidgetConfigRestrictedDomainFailsWhenOriginNotAllowedTest() throws Exception {
        mockMvc.perform(get("/api/v1/public/widget/config")
                        .param("channelKey", restrictedChannelKey)
                        .header("Origin", "https://hacker.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên miền của bạn không được phép sử dụng Widget này"));
    }

    @Test
    void getWidgetConfigFailsWhenChannelInactiveTest() throws Exception {
        mockMvc.perform(get("/api/v1/public/widget/config")
                        .param("channelKey", inactiveChannelKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kênh kết nối hiện không hoạt động"));
    }

    @Test
    void getWidgetConfigFailsWhenChannelNotFoundTest() throws Exception {
        mockMvc.perform(get("/api/v1/public/widget/config")
                        .param("channelKey", "chk_nonexistent123456"))
                .andExpect(status().isNotFound());
    }

    @Test
    void handshakeCreatesNewVisitorAndUpdatesChannelConnectionTest() throws Exception {
        WidgetHandshakeRequest request = new WidgetHandshakeRequest(validChannelKey, null, null, null);

        MvcResult result = mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://customer-site.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.channelKey").value(validChannelKey))
                .andExpect(jsonPath("$.data.channelName").value("Open Web Channel"))
                .andExpect(jsonPath("$.data.welcomeMessage").value("Chào mừng bạn!"))
                .andExpect(jsonPath("$.data.aiEnabled").value(true))
                .andExpect(jsonPath("$.data.visitorId").isNotEmpty())
                .andExpect(jsonPath("$.data.visitorToken").isNotEmpty())
                .andExpect(jsonPath("$.data.visitorName").value("Khách truy cập"))
                .andExpect(jsonPath("$.data.isNewVisitor").value(true))
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty())
                .andExpect(jsonPath("$.data.conversationStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.handlingMode").value("AI"))
                .andExpect(jsonPath("$.data.isNewConversation").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String visitorToken = root.path("data").path("visitorToken").asText();
        assertTrue(visitorToken.startsWith("vst_"));

        // Kiểm tra Channel đã được cập nhật firstConnectedAt và lastSeenAt
        Channel updatedChannel = channelRepository.findByChannelKey(validChannelKey).orElseThrow();
        assertNotNull(updatedChannel.getFirstConnectedAt());
        assertNotNull(updatedChannel.getLastSeenAt());
    }

    @Test
    void handshakeRestoresExistingVisitorAndUpdatesVisitorInfoTest() throws Exception {
        // Lần 1: Handshake tạo visitor mới
        WidgetHandshakeRequest firstReq = new WidgetHandshakeRequest(restrictedChannelKey, null, "Nguyễn Văn A", "a@example.com");

        MvcResult firstResult = mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://shop.example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewVisitor").value(true))
                .andExpect(jsonPath("$.data.visitorName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.visitorEmail").value("a@example.com"))
                .andExpect(jsonPath("$.data.conversationStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.handlingMode").value("HUMAN"))
                .andExpect(jsonPath("$.data.isNewConversation").value(true))
                .andReturn();

        JsonNode firstData = objectMapper.readTree(firstResult.getResponse().getContentAsString()).path("data");
        long firstVisitorId = firstData.path("visitorId").asLong();
        long firstConversationId = firstData.path("conversationId").asLong();
        String visitorToken = firstData.path("visitorToken").asText();

        Channel channelAfterFirst = channelRepository.findByChannelKey(restrictedChannelKey).orElseThrow();
        LocalDateTime firstConnectedAt = channelAfterFirst.getFirstConnectedAt();
        assertNotNull(firstConnectedAt);

        // Lần 2: Handshake gửi lại visitorToken cũ, cập nhật tên mới
        WidgetHandshakeRequest secondReq = new WidgetHandshakeRequest(restrictedChannelKey, visitorToken, "Nguyễn Văn A (VIP)", null);

        mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://shop.example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewVisitor").value(false))
                .andExpect(jsonPath("$.data.visitorId").value(firstVisitorId))
                .andExpect(jsonPath("$.data.visitorToken").value(visitorToken))
                .andExpect(jsonPath("$.data.visitorName").value("Nguyễn Văn A (VIP)"))
                .andExpect(jsonPath("$.data.visitorEmail").value("a@example.com"))
                .andExpect(jsonPath("$.data.conversationId").value(firstConversationId))
                .andExpect(jsonPath("$.data.isNewConversation").value(false));

        // Kiểm tra firstConnectedAt của Channel vẫn giữ nguyên không bị ghi đè
        Channel channelAfterSecond = channelRepository.findByChannelKey(restrictedChannelKey).orElseThrow();
        assertEquals(firstConnectedAt, channelAfterSecond.getFirstConnectedAt());
        assertNotNull(channelAfterSecond.getLastSeenAt());
    }

    @Test
    void handshakeWithUnknownVisitorTokenCreatesNewVisitorTest() throws Exception {
        WidgetHandshakeRequest request = new WidgetHandshakeRequest(validChannelKey, "vst_invalid_or_expired_token", null, null);

        mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://anywebsite.vn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewVisitor").value(true))
                .andExpect(jsonPath("$.data.visitorToken").isNotEmpty());
    }

    @Test
    void handshakeFailsWhenOriginNotAllowedTest() throws Exception {
        WidgetHandshakeRequest request = new WidgetHandshakeRequest(restrictedChannelKey, null, null, null);

        mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://evil-site.org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên miền của bạn không được phép sử dụng Widget này"));
    }

    @Test
    void handshakeFailsWhenChannelInactiveTest() throws Exception {
        WidgetHandshakeRequest request = new WidgetHandshakeRequest(inactiveChannelKey, null, null, null);

        mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kênh kết nối hiện không hoạt động"));
    }

    @Test
    void restoreConversationOnWidgetReturnsConversationAndMessagesTest() throws Exception {
        // 1. Handshake tạo Visitor + Conversation
        WidgetHandshakeRequest handshakeReq = new WidgetHandshakeRequest(validChannelKey, null, "Trần Văn Khách", "tran@example.com");
        MvcResult handshakeResult = mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://customer-site.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handshakeReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode handshakeData = objectMapper.readTree(handshakeResult.getResponse().getContentAsString()).path("data");
        String visitorToken = handshakeData.path("visitorToken").asText();
        long conversationId = handshakeData.path("conversationId").asLong();
        long visitorId = handshakeData.path("visitorId").asLong();

        // 2. Giả lập có 2 tin nhắn và cuộc hội thoại đã RESOLVED
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        Company company = conversation.getChannel().getCompany();

        ConversationMessage msg1 = new ConversationMessage();
        msg1.setCompany(company);
        msg1.setConversation(conversation);
        msg1.setSenderType("VISITOR");
        msg1.setSenderId(visitorId);
        msg1.setMessageType("TEXT");
        msg1.setContent("Xin chào shop, mình cần tư vấn sản phẩm");
        conversationMessageRepository.save(msg1);

        ConversationMessage msg2 = new ConversationMessage();
        msg2.setCompany(company);
        msg2.setConversation(conversation);
        msg2.setSenderType("AI");
        msg2.setSenderId(null);
        msg2.setMessageType("TEXT");
        msg2.setContent("Chào bạn! Shop có thể giúp gì cho bạn ạ?");
        conversationMessageRepository.save(msg2);

        conversation.setStatus(ConversationStatus.RESOLVED);
        conversation.setResolvedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 3. Gọi API Restore Conversation từ Widget -> Mở lại OPEN và trả về 2 tin nhắn cũ
        mockMvc.perform(get("/api/v1/public/widget/conversations/restore")
                        .param("channelKey", validChannelKey)
                        .param("visitorToken", visitorToken)
                        .header("Origin", "https://customer-site.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.conversation.id").value(conversationId))
                .andExpect(jsonPath("$.data.conversation.status").value("OPEN"))
                .andExpect(jsonPath("$.data.messages", hasSize(2)))
                .andExpect(jsonPath("$.data.messages[0].senderType").value("VISITOR"))
                .andExpect(jsonPath("$.data.messages[0].content").value("Xin chào shop, mình cần tư vấn sản phẩm"))
                .andExpect(jsonPath("$.data.messages[1].senderType").value("AI"))
                .andExpect(jsonPath("$.data.messages[1].content").value("Chào bạn! Shop có thể giúp gì cho bạn ạ?"));
    }

    @Test
    void customerSendMessageOnWidgetSuccessTest() throws Exception {
        // 1. Handshake tạo Visitor + Conversation
        WidgetHandshakeRequest handshakeReq = new WidgetHandshakeRequest(validChannelKey, null, "Phạm Thị Hoa", "hoa@example.com");
        MvcResult handshakeResult = mockMvc.perform(post("/api/v1/public/widget/handshake")
                        .header("Origin", "https://customer-site.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handshakeReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode handshakeData = objectMapper.readTree(handshakeResult.getResponse().getContentAsString()).path("data");
        String visitorToken = handshakeData.path("visitorToken").asText();
        long conversationId = handshakeData.path("conversationId").asLong();
        long visitorId = handshakeData.path("visitorId").asLong();

        // 2. Khách hàng gửi tin nhắn qua Widget
        WidgetSendMessageRequest sendReq = new WidgetSendMessageRequest(
                validChannelKey,
                visitorToken,
                "Shop ơi mẫu áo này còn size M không?",
                "TEXT"
        );

        mockMvc.perform(post("/api/v1/public/widget/messages")
                        .header("Origin", "https://customer-site.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.senderType").value("VISITOR"))
                .andExpect(jsonPath("$.data.senderId").value(visitorId))
                .andExpect(jsonPath("$.data.content").value("Shop ơi mẫu áo này còn size M không?"))
                .andExpect(jsonPath("$.data.messageType").value("TEXT"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        // 3. Kiểm tra Restore lại cuộc hội thoại có chứa tin nhắn vừa gửi
        mockMvc.perform(get("/api/v1/public/widget/conversations/restore")
                        .param("channelKey", validChannelKey)
                        .param("visitorToken", visitorToken)
                        .header("Origin", "https://customer-site.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[0].content").value("Shop ơi mẫu áo này còn size M không?"));
    }
}
