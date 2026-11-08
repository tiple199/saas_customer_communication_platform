package vn.lnt.saas_customer_communication_platform.feature.subscription.controller;

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
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PlanControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (!userRepository.existsByEmail("superadmin@example.com")) {
            User superAdmin = new User("superadmin@example.com", "password123", "Super Admin", "ROLE_SUPER_ADMIN");
            userRepository.save(superAdmin);
        }
        if (!userRepository.existsByEmail("normalplanuser@example.com")) {
            User normalUser = new User("normalplanuser@example.com", "password123", "Normal User", "ROLE_USER");
            userRepository.save(normalUser);
        }
    }

    @Test
    void crudPlanForSuperAdminSuccessTest() throws Exception {
        // 1. Create Plan by Super Admin
        String createJson = """
                {
                    "name": "Pro Plan Test",
                    "price": 499000.00,
                    "maxUser": 20,
                    "maxChannel": 5,
                    "aiEnabled": true,
                    "aiQuota": 5000,
                    "storageLimit": 1024,
                    "status": "ACTIVE"
                }
                """;

        String responseContent = mockMvc.perform(post("/api/v1/plans")
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.name").value("Pro Plan Test"))
                .andExpect(jsonPath("$.data.maxUser").value(20))
                .andExpect(jsonPath("$.data.maxChannel").value(5))
                .andExpect(jsonPath("$.data.aiEnabled").value(true))
                .andExpect(jsonPath("$.data.aiQuota").value(5000))
                .andExpect(jsonPath("$.data.storageLimit").value(1024))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        com.jayway.jsonpath.ReadContext ctx = com.jayway.jsonpath.JsonPath.parse(responseContent);
        Number idNum = ctx.read("$.data.id");
        Long planId = idNum.longValue();

        // 2. Duplicate Plan Name should fail with 409 Conflict
        mockMvc.perform(post("/api/v1/plans")
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isConflict());

        // 3. Get Plan by Id
        mockMvc.perform(get("/api/v1/plans/" + planId)
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Pro Plan Test"));

        // 4. Get Active Plans (accessible by Normal User)
        mockMvc.perform(get("/api/v1/plans/active")
                        .with(jwt().jwt(j -> j.subject("normalplanuser@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 5. Get All Plans & Paged Plans by Super Admin
        mockMvc.perform(get("/api/v1/plans")
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/plans/paged")
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com")))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))));

        // 6. Update Plan by Super Admin
        String updateJson = """
                {
                    "name": "Enterprise Plan Updated",
                    "price": 999000.00,
                    "maxUser": 50,
                    "maxChannel": 15,
                    "aiEnabled": true,
                    "aiQuota": 20000,
                    "storageLimit": 5120
                }
                """;

        mockMvc.perform(put("/api/v1/plans/" + planId)
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Enterprise Plan Updated"))
                .andExpect(jsonPath("$.data.maxUser").value(50))
                .andExpect(jsonPath("$.data.aiQuota").value(20000));

        // 7. Toggle Plan Status to INACTIVE
        mockMvc.perform(patch("/api/v1/plans/" + planId + "/status")
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "INACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // 8. Delete Plan by Super Admin
        mockMvc.perform(delete("/api/v1/plans/" + planId)
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // 9. Verify Deleted (404)
        mockMvc.perform(get("/api/v1/plans/" + planId)
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalUserCannotManagePlansTest() throws Exception {
        String createJson = """
                {
                    "name": "Unauthorized Plan",
                    "price": 100000.00,
                    "maxUser": 5,
                    "maxChannel": 2,
                    "aiEnabled": false,
                    "storageLimit": 100
                }
                """;

        mockMvc.perform(post("/api/v1/plans")
                        .with(jwt().jwt(j -> j.subject("normalplanuser@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ Super Admin mới có quyền quản lý gói dịch vụ (Plan)"));

        mockMvc.perform(get("/api/v1/plans")
                        .with(jwt().jwt(j -> j.subject("normalplanuser@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ Super Admin mới có quyền quản lý gói dịch vụ (Plan)"));
    }

    @Test
    void cannotDeletePlanInUseBySubscriptionTest() throws Exception {
        String createJson = """
                {
                    "name": "Subscribed Plan",
                    "price": 199000.00,
                    "maxUser": 10,
                    "maxChannel": 3,
                    "aiEnabled": true,
                    "aiQuota": 1000,
                    "storageLimit": 500
                }
                """;

        String responseContent = mockMvc.perform(post("/api/v1/plans")
                        .with(jwt().jwt(j -> j.subject("superadmin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number idNum = com.jayway.jsonpath.JsonPath.parse(responseContent).read("$.data.id");
        Long planId = idNum.longValue();

        Plan plan = planRepository.findById(planId).orElseThrow();
        Subscription sub = new Subscription();
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        Subscription savedSub = subscriptionRepository.save(sub);

        try {
            mockMvc.perform(delete("/api/v1/plans/" + planId)
                            .with(jwt().jwt(j -> j.subject("superadmin@example.com"))))
                    .andExpect(status().isBadRequest());
        } finally {
            subscriptionRepository.delete(savedSub);
            planRepository.deleteById(planId);
        }
    }
}

