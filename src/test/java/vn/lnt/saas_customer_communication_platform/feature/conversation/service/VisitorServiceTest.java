package vn.lnt.saas_customer_communication_platform.feature.conversation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CreateCompanyRequest;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.service.CompanyService;
import vn.lnt.saas_customer_communication_platform.feature.conversation.dto.VisitorResolutionResult;
import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;
import vn.lnt.saas_customer_communication_platform.feature.conversation.service.impl.VisitorServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VisitorServiceTest {

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyService companyService;

    private Company companyA;
    private Company companyB;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("visitorowner@example.com")) {
            User user = new User("visitorowner@example.com", "password123", "Visitor Owner", "ROLE_USER");
            userRepository.save(user);
        }

        CompanyResponse respA = companyService.createCompany("visitorowner@example.com", new CreateCompanyRequest("Visitor Corp A"));
        this.companyA = companyRepository.findById(respA.getId()).orElseThrow();

        CompanyResponse respB = companyService.createCompany("visitorowner@example.com", new CreateCompanyRequest("Visitor Corp B"));
        this.companyB = companyRepository.findById(respB.getId()).orElseThrow();
    }

    @Test
    void createVisitorWithDefaultNameWhenNameOmittedTest() {
        Visitor visitor = visitorService.createVisitor(companyA, null, null);

        assertNotNull(visitor.getId());
        assertNotNull(visitor.getVisitorToken());
        assertTrue(visitor.getVisitorToken().startsWith("vst_"));
        assertEquals(VisitorServiceImpl.DEFAULT_VISITOR_NAME, visitor.getName());
        assertNull(visitor.getEmail());
        assertEquals(companyA.getId(), visitor.getCompany().getId());
        assertNotNull(visitor.getCreatedAt());
        assertNotNull(visitor.getLastSeenAt());
    }

    @Test
    void createVisitorWithProvidedNameAndEmailTest() {
        Visitor visitor = visitorService.createVisitor(companyA, "  Trần Thị B  ", "  b@example.com  ");

        assertNotNull(visitor.getId());
        assertEquals("Trần Thị B", visitor.getName());
        assertEquals("b@example.com", visitor.getEmail());
    }

    @Test
    void createVisitorWithoutCompanyThrowsExceptionTest() {
        assertThrows(InvalidOperationException.class, () ->
                visitorService.createVisitor(null, "Test", "test@example.com"));
    }

    @Test
    void resolveOrCreateVisitorRestoresSameCompanyVisitorAndIsolatesCrossTenantTest() {
        // 1. Tạo mới qua resolveOrCreateVisitor
        VisitorResolutionResult first = visitorService.resolveOrCreateVisitor(companyA, null, "Khách A", "a@corp.com");
        assertTrue(first.isNewVisitor());
        String token = first.getVisitor().getVisitorToken();

        // 2. Khôi phục với cùng companyA -> trả về đúng visitor cũ
        VisitorResolutionResult second = visitorService.resolveOrCreateVisitor(companyA, token, "Khách A Updated", null);
        assertFalse(second.isNewVisitor());
        assertEquals(first.getVisitor().getId(), second.getVisitor().getId());
        assertEquals("Khách A Updated", second.getVisitor().getName());
        assertEquals("a@corp.com", second.getVisitor().getEmail());

        // 3. Nếu gửi token của companyA sang companyB -> Không được phép lấy visitor của companyA mà phải tạo mới cho companyB
        VisitorResolutionResult crossTenant = visitorService.resolveOrCreateVisitor(companyB, token, null, null);
        assertTrue(crossTenant.isNewVisitor());
        assertNotEquals(first.getVisitor().getId(), crossTenant.getVisitor().getId());
        assertEquals(companyB.getId(), crossTenant.getVisitor().getCompany().getId());
    }

    @Test
    void getAndUpdateVisitorInfoSuccessAndTenantIsolationTest() {
        Visitor visitor = visitorService.createVisitor(companyA, "Initial Name", null);

        Visitor updated = visitorService.updateVisitorInfo(companyA.getId(), visitor.getVisitorToken(), "New Name", "new@example.com");
        assertEquals("New Name", updated.getName());
        assertEquals("new@example.com", updated.getEmail());

        // Truy vấn bằng companyB phải báo ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                visitorService.getVisitorByToken(companyB.getId(), visitor.getVisitorToken()));
        assertThrows(ResourceNotFoundException.class, () ->
                visitorService.getVisitorById(companyB.getId(), visitor.getId()));
    }
}

