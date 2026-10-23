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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

        companyService.switchCompany("chatowner@example.com", comp.getId());

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
                        .param("companyId", comp.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoomJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.name").value("Team Dev Chat"))
                .andExpect(jsonPath("$.data.type").value("GROUP"))
                .andReturn().getResponse().getContentAsString();

        Object roomIdObj = com.jayway.jsonpath.JsonPath.read(resStr, "$.data.id");

        // Test get my chat rooms
        mockMvc.perform(get("/api/v1/chat-rooms")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Team Dev Chat"));

        // Test get chat room by ID
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomIdObj)
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("Team Dev Chat"))
                .andExpect(jsonPath("$.data.type").value("GROUP"));

        // Test get chat room members
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomIdObj + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void addAndRemoveGroupMembersTest() throws Exception {
        // Setup 3rd user
        if (!userRepository.existsByEmail("chatmember2@example.com")) {
            userRepository.save(new User("chatmember2@example.com", "password123", "Chat Member 2", "ROLE_USER"));
        }
        User member2 = userRepository.findByEmail("chatmember2@example.com").orElseThrow();
        User member1 = userRepository.findByEmail("chatmember@example.com").orElseThrow();

        vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse comp =
                companyService.createCompany("chatowner@example.com", new CreateCompanyRequest("Group Test Corp"));

        companyService.addMember("chatowner@example.com", comp.getId(),
                new vn.lnt.saas_customer_communication_platform.feature.company.dto.AddMemberRequest("chatmember@example.com", vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole.STAFF));
        companyService.addMember("chatowner@example.com", comp.getId(),
                new vn.lnt.saas_customer_communication_platform.feature.company.dto.AddMemberRequest("chatmember2@example.com", vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole.STAFF));

        companyService.switchCompany("chatowner@example.com", comp.getId());
        companyService.switchCompany("chatmember@example.com", comp.getId());

        // Create GROUP room with owner and member1
        String createJson = String.format("""
                {
                    "name": "Backend Team",
                    "type": "GROUP",
                    "memberUserIds": [%d]
                }
                """, member1.getId());

        String resStr = mockMvc.perform(post("/api/v1/chat-rooms")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Object roomId = com.jayway.jsonpath.JsonPath.read(resStr, "$.data.id");

        // 1. Add member2 to group
        String addMemberJson = String.format("""
                {
                    "userIds": [%d]
                }
                """, member2.getId());

        mockMvc.perform(post("/api/v1/chat-rooms/" + roomId + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addMemberJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Verify members count is now 3
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomId + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        // 2. Add member2 again -> should fail with 400 Bad Request
        mockMvc.perform(post("/api/v1/chat-rooms/" + roomId + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addMemberJson))
                .andExpect(status().isBadRequest());

        // 3. Member1 (non-creator) tries to remove member2 -> should fail with 400
        mockMvc.perform(delete("/api/v1/chat-rooms/" + roomId + "/members/" + member2.getId())
                        .with(jwt().jwt(builder -> builder.subject("chatmember@example.com"))))
                .andExpect(status().isBadRequest());

        // 4. Creator removes member2 -> success
        mockMvc.perform(delete("/api/v1/chat-rooms/" + roomId + "/members/" + member2.getId())
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Verify member count is 2
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomId + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 5. Creator tries to remove themselves (leave) -> should fail
        User owner = userRepository.findByEmail("chatowner@example.com").orElseThrow();
        mockMvc.perform(delete("/api/v1/chat-rooms/" + roomId + "/members/" + owner.getId())
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isBadRequest());

        // 6. Member1 leaves the room -> success
        mockMvc.perform(delete("/api/v1/chat-rooms/" + roomId + "/members/" + member1.getId())
                        .with(jwt().jwt(builder -> builder.subject("chatmember@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Verify member count is now 1 (only creator left)
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomId + "/members")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void sendMessageAndGetMessagesTest() throws Exception {
        User member = userRepository.findByEmail("chatmember@example.com").orElseThrow();

        vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse comp =
                companyService.createCompany("chatowner@example.com", new CreateCompanyRequest("Message Corp"));

        companyService.addMember("chatowner@example.com", comp.getId(),
                new vn.lnt.saas_customer_communication_platform.feature.company.dto.AddMemberRequest("chatmember@example.com", vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole.STAFF));

        companyService.switchCompany("chatowner@example.com", comp.getId());
        companyService.switchCompany("chatmember@example.com", comp.getId());

        // Create GROUP room
        String createJson = String.format("""
                {
                    "name": "General Chat",
                    "type": "GROUP",
                    "memberUserIds": [%d]
                }
                """, member.getId());

        String resStr = mockMvc.perform(post("/api/v1/chat-rooms")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Object roomId = com.jayway.jsonpath.JsonPath.read(resStr, "$.data.id");

        // 1. Chatowner sends message
        String sendJson1 = """
                {
                    "content": "Xin chào cả nhóm!",
                    "messageType": "TEXT"
                }
                """;
        mockMvc.perform(post("/api/v1/chat-rooms/" + roomId + "/messages")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendJson1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.content").value("Xin chào cả nhóm!"))
                .andExpect(jsonPath("$.data.senderId").isNotEmpty());

        // 2. Chatmember sends reply
        String sendJson2 = """
                {
                    "content": "Chào sếp!"
                }
                """;
        mockMvc.perform(post("/api/v1/chat-rooms/" + roomId + "/messages")
                        .with(jwt().jwt(builder -> builder.subject("chatmember@example.com")))
                        .param("companyId", comp.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendJson2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.content").value("Chào sếp!"))
                .andExpect(jsonPath("$.data.senderId").value(member.getId()));

        // 3. Get messages in room
        mockMvc.perform(get("/api/v1/chat-rooms/" + roomId + "/messages")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].content").value("Chào sếp!"))
                .andExpect(jsonPath("$.data.items[1].content").value("Xin chào cả nhóm!"));

        // 4. Blank message fails
        String blankJson = """
                {
                    "content": "   "
                }
                """;
        mockMvc.perform(post("/api/v1/chat-rooms/" + roomId + "/messages")
                        .with(jwt().jwt(builder -> builder.subject("chatowner@example.com")))
                        .param("companyId", comp.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankJson))
                .andExpect(status().isBadRequest());
    }
}

