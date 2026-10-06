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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("ACME Corp"))
                .andExpect(jsonPath("$.data[0].userRole").value("OWNER"));

        // Test get company by ID
        mockMvc.perform(get("/api/v1/companies/1")
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

        mockMvc.perform(put("/api/v1/companies/1")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateCompanyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("ACME Corp Updated"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/logo.png"));

        // Test switch company
        mockMvc.perform(post("/api/v1/companies/1/switch")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Chuyển công ty thành công"))
                .andExpect(jsonPath("$.data.id").value(1));

        // Test get company members by ID
        mockMvc.perform(get("/api/v1/companies/1/members")
                        .with(jwt().jwt(builder -> builder.subject("testuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email").value("testuser@example.com"))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"));
    }
}
