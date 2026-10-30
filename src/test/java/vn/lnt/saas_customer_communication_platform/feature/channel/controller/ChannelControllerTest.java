package vn.lnt.saas_customer_communication_platform.feature.channel.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.service.CompanyService;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ChannelControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    private Long companyId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (!userRepository.existsByEmail("channelowner@example.com")) {
            User user = new User("channelowner@example.com", "password123", "Channel Owner", "ROLE_USER");
            userRepository.save(user);
        }
        if (!userRepository.existsByEmail("channelmember@example.com")) {
            User member = new User("channelmember@example.com", "password123", "Channel Member", "ROLE_USER");
            userRepository.save(member);
        }

        CompanyResponse company = companyService.createCompany("channelowner@example.com", new CreateCompanyRequest("Channel Corp"));
        this.companyId = company.getId();
        companyService.switchCompany("channelowner@example.com", this.companyId);
    }

    @Test
    void crudChannelSuccessTest() throws Exception {
        // 1. Create Channel
        String createJson = """
                {
                    "name": "Livechat Widget",
                    "type": "WEB",
                    "config": {
                        "welcomeMessage": "Xin chào"
                    }
                }
                """;

        String responseContent = mockMvc.perform(post("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.name").value("Livechat Widget"))
                .andExpect(jsonPath("$.data.type").value("WEB"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.channelKey").isNotEmpty())
                .andExpect(jsonPath("$.data.channelKey").value(startsWith("chk_")))
                .andExpect(jsonPath("$.data.config.welcomeMessage").value("Xin chào"))
                .andExpect(jsonPath("$.data.config.widgetKey").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Extract channel id and channel key
        com.jayway.jsonpath.ReadContext ctx = com.jayway.jsonpath.JsonPath.parse(responseContent);
        Number idNum = ctx.read("$.data.id");
        Long channelId = idNum.longValue();
        String channelKey = ctx.read("$.data.channelKey");

        // 2. Get Channel By Id
        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("Livechat Widget"))
                .andExpect(jsonPath("$.data.channelKey").value(channelKey))
                .andExpect(jsonPath("$.data.config.widgetKey").isNotEmpty());

        // 2b. Get Channel By Key
        mockMvc.perform(get("/api/v1/channels/key/" + channelKey)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(channelId))
                .andExpect(jsonPath("$.data.channelKey").value(channelKey));

        // 2c. Regenerate Channel Key
        mockMvc.perform(post("/api/v1/channels/" + channelId + "/regenerate-key")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.channelKey").isNotEmpty())
                .andExpect(jsonPath("$.data.channelKey").value(startsWith("chk_")));

        // 3. Get Channels list
        mockMvc.perform(get("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 3b. Get Channels paged
        mockMvc.perform(get("/api/v1/channels/paged")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));

        // 4. Update Channel
        String updateJson = """
                {
                    "name": "Livechat Official",
                    "type": "TELEGRAM",
                    "status": "INACTIVE",
                    "config": {
                        "botToken": "123456:ABC-DEF"
                    }
                }
                """;

        mockMvc.perform(put("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("Livechat Official"))
                .andExpect(jsonPath("$.data.type").value("TELEGRAM"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.config.botToken").value("123456:ABC-DEF"));

        // 5. Delete Channel
        mockMvc.perform(delete("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // 6. Verify Deleted (should return 404)
        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTelegramChannelSuccessTest() throws Exception {
        String createJson = """
                {
                    "name": "Telegram Bot Channel",
                    "type": "TELEGRAM",
                    "config": {
                        "botToken": "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ",
                        "botUsername": "CustomerSupportBot"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.type").value("TELEGRAM"))
                .andExpect(jsonPath("$.data.config.botUsername").value("CustomerSupportBot"));
    }

    @Test
    void createTelegramChannelWithoutBotTokenFailsTest() throws Exception {
        String createJson = """
                {
                    "name": "Telegram Without Token",
                    "type": "TELEGRAM"
                }
                """;

        mockMvc.perform(post("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bot token của Telegram là bắt buộc và không được để trống"));
    }

    @Test
    void createChannelWithInvalidTypeFailsTest() throws Exception {
        String invalidJson = """
                {
                    "name": "Invalid Channel",
                    "type": "INVALID_TYPE_123"
                }
                """;

        mockMvc.perform(post("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMemberCannotAccessChannelTest() throws Exception {
        mockMvc.perform(get("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelmember@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWebChannelWithInvalidAllowedDomainFailsTest() throws Exception {
        String invalidJson = """
                {
                    "name": "Web Bad Domain",
                    "type": "WEB",
                    "config": {
                        "allowedDomains": "invalid domain name with spaces"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Tên miền không hợp lệ")));
    }

    @Test
    void toggleChannelAiSuccessTest() throws Exception {
        // 1. Tạo channel mới
        String createJson = """
                {
                    "name": "Channel for AI Toggle",
                    "type": "WEB",
                    "aiEnabled": false
                }
                """;

        String responseContent = mockMvc.perform(post("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aiEnabled").value(false))
                .andReturn().getResponse().getContentAsString();

        com.jayway.jsonpath.ReadContext ctx = com.jayway.jsonpath.JsonPath.parse(responseContent);
        Number idNum = ctx.read("$.data.id");
        Long channelId = idNum.longValue();

        // 2. Bật AI cho channel
        mockMvc.perform(patch("/api/v1/channels/" + channelId + "/ai")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bật tính năng AI cho kênh kết nối thành công"))
                .andExpect(jsonPath("$.data.aiEnabled").value(true));

        // 3. Kiểm tra lại thông tin channel xem aiEnabled đã đổi thành true chưa
        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiEnabled").value(true));

        // 4. Tắt AI cho channel
        mockMvc.perform(patch("/api/v1/channels/" + channelId + "/ai")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tắt tính năng AI cho kênh kết nối thành công"))
                .andExpect(jsonPath("$.data.aiEnabled").value(false));

        // 5. Kiểm tra lại sau khi tắt
        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiEnabled").value(false));
    }

    @Test
    void toggleChannelAiFailsWithoutBodyTest() throws Exception {
        mockMvc.perform(patch("/api/v1/channels/1/ai")
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
