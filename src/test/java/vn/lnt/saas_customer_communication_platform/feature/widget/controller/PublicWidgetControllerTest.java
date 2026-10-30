package vn.lnt.saas_customer_communication_platform.feature.widget.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PublicWidgetControllerTest {

    private MockMvc mockMvc;

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
}
