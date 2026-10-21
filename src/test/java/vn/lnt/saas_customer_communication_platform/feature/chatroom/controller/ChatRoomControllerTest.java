package vn.lnt.saas_customer_communication_platform.feature.chatroom.controller;

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
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.service.CompanyService;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ChatRoomControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (!userRepository.existsByEmail("chatowner@example.com")) {
            User user = new User("chatowner@example.com", "password123", "Chat Owner", "ROLE_USER");
            userRepository.save(user);
        }
        if (!userRepository.existsByEmail("chatmember@example.com")) {
            User member = new User("chatmember@example.com", "password123", "Chat Member", "ROLE_USER");
            userRepository.save(member);
        }
    }

    @Test
    void createChatRoomAndGetRoomsSuccess() throws Exception {
        // Create company for chatowner
        vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse comp = companyService.createCompany("chatowner@example.com", new CreateCompanyRequest("Chat Corp"));

        // Add chatmember to Chat Corp
        companyService.addMember("chatowner@example.com", comp.getId(),
                new vn.lnt.saas_customer_communication_platform.feature.company.dto.AddMemberRequest("chatmember@example.com", vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole.STAFF));

        User chatMember = userRepository.findByEmail("chatmember@example.com").orElseThrow();

        String createRoomJson = String.format("""
                {
                    "name": "Team Dev Chat",
                    "type": "GROUP",
                    "memberUserIds": [%d]
                }
                """, chatMember.getId());

        String resStr = mockMvc.perform(post("/api/v1/chat-rooms")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoomJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.name").value("Team Dev Chat"))
                .andExpect(jsonPath("$.data.type").value("GROUP"))
                .andExpect(jsonPath("$.data.name").value("Team Dev Chat"))
                .andReturn().getResponse().getContentAsString();

        Object roomIdObj = com.jayway.jsonpath.JsonPath.read(resStr, "$.data.id");

        // Test get my chat rooms
        mockMvc.perform(get("/api/v1/chat-rooms")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Team Dev Chat"))
                .andExpect(jsonPath("$.data[0].name").value("Team Dev Chat"));

        // Test get chat room by ID
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomIdObj)
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("Team Dev Chat"))
                .andExpect(jsonPath("$.data.type").value("GROUP"));

        // Test get chat room members
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomIdObj + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
