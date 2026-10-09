package vn.lnt.saas_customer_communication_platform.feature.company.controller;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CompanyControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (!userRepository.existsByEmail("testuser@example.com")) {
            User user = new User("testuser@example.com", "password123", "Test User", "ROLE_USER");
            userRepository.save(user);
        }
        if (!userRepository.existsByEmail("memberuser@example.com")) {
            User memberUser = new User("memberuser@example.com", "password123", "Member User", "ROLE_USER");
            userRepository.save(memberUser);
        }
    }

    @Test
    void createCompanyAndGetMyCompaniesSuccess() throws Exception {
        String createCompanyJson = """
                {
                    "name": "ACME Corp"
                }
                """;

        String responseStr = mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCompanyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.name").value("ACME Corp"))
                .andExpect(jsonPath("$.data.userRole").value("OWNER"))
                .andReturn().getResponse().getContentAsString();

        Object compIdObj = com.jayway.jsonpath.JsonPath.read(responseStr, "$.data.id");
        User testUser = userRepository.findByEmail("testuser@example.com").orElseThrow();

        mockMvc.perform(get("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("ACME Corp"))
                .andExpect(jsonPath("$.data[0].userRole").value("OWNER"));

        // Test get company by ID
        mockMvc.perform(get("/api/v1/companies/" + compIdObj)
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("ACME Corp"));

        // Test update company
        String updateCompanyJson = """
                {
                    "name": "ACME Corp Updated",
                    "logoUrl": "https://example.com/logo.png"
                }
                """;

        mockMvc.perform(put("/api/v1/companies/" + compIdObj)
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateCompanyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("ACME Corp Updated"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/logo.png"));

        // Test switch company
        mockMvc.perform(post("/api/v1/companies/" + compIdObj + "/switch")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Chuyển công ty thành công"))
                .andExpect(jsonPath("$.data.id").value(compIdObj));

        // Test add member
        String addMemberJson = """
                {
                    "email": "memberuser@example.com",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/companies/" + compIdObj + "/members")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addMemberJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.email").value("memberuser@example.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // Test get company members by ID
        mockMvc.perform(get("/api/v1/companies/" + compIdObj + "/members")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));

        User memberUser = userRepository.findByEmail("memberuser@example.com").orElseThrow();

        // ADMIN tries to update status of OWNER (higher priority) -> Should fail
        String deactivateJson = """
                {
                    "status": "INACTIVE"
                }
                """;
        mockMvc.perform(patch("/api/v1/companies/" + compIdObj + "/members/" + testUser.getId() + "/status")
                        .with(jwt().jwt(builder -> builder.subject("memberuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bạn chỉ được phép cập nhật trạng thái thành viên có cấp bậc thấp hơn mình"));

        // OWNER updates ADMIN status to INACTIVE -> Should succeed
        mockMvc.perform(patch("/api/v1/companies/" + compIdObj + "/members/" + memberUser.getId() + "/status")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // OWNER updates ADMIN status back to ACTIVE -> Should succeed
        String activateJson = """
                {
                    "status": "ACTIVE"
                }
                """;
        mockMvc.perform(patch("/api/v1/companies/" + compIdObj + "/members/" + memberUser.getId() + "/status")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // ADMIN tries to kick OWNER (higher priority) -> Should fail
        mockMvc.perform(delete("/api/v1/companies/" + compIdObj + "/members/" + testUser.getId())
                        .with(jwt().jwt(builder -> builder.subject("memberuser@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bạn chỉ được phép xóa thành viên có cấp bậc thấp hơn mình"));

        // ADMIN tries to kick self (equal priority) -> Should fail
        mockMvc.perform(delete("/api/v1/companies/" + compIdObj + "/members/" + memberUser.getId())
                        .with(jwt().jwt(builder -> builder.subject("memberuser@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bạn chỉ được phép xóa thành viên có cấp bậc thấp hơn mình"));

        // OWNER kicks ADMIN (lower priority) -> Should succeed
        mockMvc.perform(delete("/api/v1/companies/" + compIdObj + "/members/" + memberUser.getId())
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Xóa thành viên khỏi công ty thành công"));

        // Verify member count back to 1
        mockMvc.perform(get("/api/v1/companies/" + compIdObj + "/members")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void crossCompanyAccessDenied() throws Exception {
        if (!userRepository.existsByEmail("companyowner@example.com")) {
            User companyOwner = new User("companyowner@example.com", "password123", "Owner User", "ROLE_USER");
            userRepository.save(companyOwner);
        }
        if (!userRepository.existsByEmail("otheruser@example.com")) {
            User otherUser = new User("otheruser@example.com", "password123", "Other User", "ROLE_USER");
            userRepository.save(otherUser);
        }

        // Create a company with companyowner
        String createCompanyJson = """
                {
                    "name": "Isolated Corp"
                }
                """;

        String resStr = mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject("companyowner@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCompanyJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Object compIdObj = com.jayway.jsonpath.JsonPath.read(resStr, "$.data.id");

        // Non-member tries to get Company details -> Should fail
        mockMvc.perform(get("/api/v1/companies/" + compIdObj)
                        .with(jwt().jwt(builder -> builder.subject("otheruser@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập công ty này"));

        // Non-member tries to get Company members -> Should fail
        mockMvc.perform(get("/api/v1/companies/" + compIdObj + "/members")
                        .with(jwt().jwt(builder -> builder.subject("otheruser@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền xem danh sách thành viên của công ty này"));
    }
}
