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
import vn.lnt.saas_customer_communication_platform.feature.widget.dto.WidgetHandshakeRequest;

import java.time.LocalDateTime;

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
                .andReturn();

        JsonNode firstData = objectMapper.readTree(firstResult.getResponse().getContentAsString()).path("data");
        long firstVisitorId = firstData.path("visitorId").asLong();
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
                .andExpect(jsonPath("$.data.visitorEmail").value("a@example.com"));

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
}
