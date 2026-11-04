package vn.lnt.saas_customer_communication_platform.feature.conversation.controller;

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
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.AssignConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.CreateConversationRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.SendConversationMessageRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.UpdateConversationStatusRequest;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Conversation;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationMessage;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.ConversationStatus;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationMessageRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.VisitorService;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ConversationControllerTest {

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
    private VisitorService visitorService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    private Long companyId;
    private Channel aiChannel;
    private Channel humanChannel;
    private Visitor visitor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (!userRepository.existsByEmail("convowner@example.com")) {
            User user = new User("convowner@example.com", "password123", "Conversation Owner", "ROLE_USER");
            userRepository.save(user);
        }
        if (!userRepository.existsByEmail("convstranger@example.com")) {
            User stranger = new User("convstranger@example.com", "password123", "Stranger User", "ROLE_USER");
            userRepository.save(stranger);
        }

        CompanyResponse companyResp = companyService.createCompany("convowner@example.com", new CreateCompanyRequest("Conv Corp"));
        this.companyId = companyResp.getId();
        companyService.switchCompany("convowner@example.com", this.companyId);

        Company company = companyRepository.findById(this.companyId).orElseThrow();

        // Kênh bật AI
        Channel ch1 = new Channel();
        ch1.setChannelKey(Channel.generateChannelKey());
        ch1.setName("AI Support Channel");
        ch1.setType(ChannelType.WEB);
        ch1.setStatus(ChannelStatus.ACTIVE);
        ch1.setAiEnabled(true);
        ch1.setCompany(company);
        this.aiChannel = channelRepository.save(ch1);

        // Kênh tắt AI
        Channel ch2 = new Channel();
        ch2.setChannelKey(Channel.generateChannelKey());
        ch2.setName("Human Support Channel");
        ch2.setType(ChannelType.TELEGRAM);
        ch2.setStatus(ChannelStatus.ACTIVE);
        ch2.setAiEnabled(false);
        ch2.setCompany(company);
        this.humanChannel = channelRepository.save(ch2);

        // Tạo Visitor mẫu
        this.visitor = visitorService.createVisitor(company, "Lê Văn Khách", "khach@example.com");
    }

    @Test
    void createAndGetConversationSuccessTest() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest(aiChannel.getId(), visitor.getId());

        // 1. Tạo Conversation trên kênh bật AI -> handlingMode = AI, status = OPEN
        MvcResult createResult = mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.handlingMode").value("AI"))
                .andExpect(jsonPath("$.data.visitorId").value(visitor.getId()))
                .andExpect(jsonPath("$.data.visitorName").value("Lê Văn Khách"))
                .andExpect(jsonPath("$.data.channelId").value(aiChannel.getId()))
                .andExpect(jsonPath("$.data.companyId").value(companyId))
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long conversationId = root.path("data").path("id").asLong();

        // 2. Gọi tạo lại cùng Visitor + Channel khi cuộc hội thoại đang OPEN -> trả về đúng conversationId cũ
        MvcResult duplicateResult = mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long secondId = objectMapper.readTree(duplicateResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertEquals(conversationId, secondId);

        // 3. Tạo Conversation trên kênh tắt AI -> handlingMode = HUMAN
        CreateConversationRequest humanReq = new CreateConversationRequest(humanChannel.getId(), visitor.getId());
        mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(humanReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.handlingMode").value("HUMAN"));

        // 4. Lấy chi tiết Conversation theo ID
        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(conversationId))
                .andExpect(jsonPath("$.data.visitorEmail").value("khach@example.com"));

        // 5. Lấy danh sách Conversations
        mockMvc.perform(get("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));

        // 6. Lấy danh sách Conversations phân trang có lọc status=OPEN
        mockMvc.perform(get("/api/v1/conversations/paged")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void nonMemberCannotAccessConversationsTest() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convstranger@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restoreResolvedConversationReopensAndReturnsMessagesTest() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest(aiChannel.getId(), visitor.getId());
        MvcResult createResult = mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        Company company = conversation.getChannel().getCompany();

        ConversationMessage message = new ConversationMessage();
        message.setCompany(company);
        message.setConversation(conversation);
        message.setSenderType("VISITOR");
        message.setSenderId(visitor.getId());
        message.setMessageType("TEXT");
        message.setContent("Cho mình hỏi thêm về bảo hành");
        conversationMessageRepository.save(message);

        conversation.setStatus(ConversationStatus.RESOLVED);
        conversation.setResolvedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Gọi API restore cuộc hội thoại
        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/restore")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.conversation.id").value(conversationId))
                .andExpect(jsonPath("$.data.conversation.status").value("OPEN"))
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[0].content").value("Cho mình hỏi thêm về bảo hành"));

        // Gọi API lấy danh sách tin nhắn của cuộc hội thoại
        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].senderType").value("VISITOR"))
                .andExpect(jsonPath("$.data[0].content").value("Cho mình hỏi thêm về bảo hành"));
    }

    @Test
    void assignStaffToConversationSuccessTest() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest(aiChannel.getId(), visitor.getId());
        MvcResult createResult = mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.handlingMode").value("AI"))
                .andReturn();

        long conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        User owner = userRepository.findByEmail("convowner@example.com").orElseThrow();
        User stranger = userRepository.findByEmail("convstranger@example.com").orElseThrow();

        // 1. Gán Staff hợp lệ (cùng công ty) -> assigneeId được cập nhật và handlingMode chuyển sang HUMAN
        AssignConversationRequest assignReq = new AssignConversationRequest(owner.getId());
        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/assign")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(conversationId))
                .andExpect(jsonPath("$.data.assigneeId").value(owner.getId()))
                .andExpect(jsonPath("$.data.handlingMode").value("HUMAN"));

        // 2. Gán cho người không thuộc công ty -> báo lỗi 400
        AssignConversationRequest invalidAssignReq = new AssignConversationRequest(stranger.getId());
        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/assign")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAssignReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffSendMessageReplySuccessTest() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest(aiChannel.getId(), visitor.getId());
        MvcResult createResult = mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        User owner = userRepository.findByEmail("convowner@example.com").orElseThrow();

        // Staff gửi tin nhắn trả lời khách hàng
        SendConversationMessageRequest replyReq = new SendConversationMessageRequest(
                "Chào bạn, nhân viên hỗ trợ xin giải đáp thắc mắc của bạn ạ!",
                "TEXT"
        );

        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.senderType").value("STAFF"))
                .andExpect(jsonPath("$.data.senderId").value(owner.getId()))
                .andExpect(jsonPath("$.data.content").value("Chào bạn, nhân viên hỗ trợ xin giải đáp thắc mắc của bạn ạ!"))
                .andExpect(jsonPath("$.data.messageType").value("TEXT"));

        // Kiểm tra Conversation đã tự động gán assigneeId và chuyển sang HUMAN
        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeId").value(owner.getId()))
                .andExpect(jsonPath("$.data.handlingMode").value("HUMAN"));
    }

    @Test
    void updateConversationStatusSuccessTest() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest(aiChannel.getId(), visitor.getId());
        MvcResult createResult = mockMvc.perform(post("/api/v1/conversations")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Chuyển trạng thái sang RESOLVED -> resolvedAt được tự động gán
        UpdateConversationStatusRequest resolveReq = new UpdateConversationStatusRequest(ConversationStatus.RESOLVED);
        mockMvc.perform(patch("/api/v1/conversations/" + conversationId + "/status")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(conversationId))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        // Chuyển trạng thái sang CLOSED
        UpdateConversationStatusRequest closeReq = new UpdateConversationStatusRequest(ConversationStatus.CLOSED);
        mockMvc.perform(patch("/api/v1/conversations/" + conversationId + "/status")
                        .with(jwt().jwt(j -> j.subject("convowner@example.com")))
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }
}

