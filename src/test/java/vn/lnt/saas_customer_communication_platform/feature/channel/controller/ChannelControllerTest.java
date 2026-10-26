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
                    "config": "{\\"theme\\": \\"blue\\"}"
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
                .andReturn().getResponse().getContentAsString();

        // Extract channel id
        com.jayway.jsonpath.ReadContext ctx = com.jayway.jsonpath.JsonPath.parse(responseContent);
        Number idNum = ctx.read("$.data.id");
        Long channelId = idNum.longValue();

        // 2. Get Channel By Id
        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .with(jwt().jwt(j -> j.subject("channelowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("Livechat Widget"));

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
                    "status": "INACTIVE"
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
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

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
    void nonMemberCannotAccessChannelTest() throws Exception {
        mockMvc.perform(get("/api/v1/channels")
                        .with(jwt().jwt(j -> j.subject("channelmember@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isBadRequest());
    }
}
